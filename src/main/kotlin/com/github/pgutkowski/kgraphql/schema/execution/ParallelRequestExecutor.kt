package com.github.pgutkowski.kgraphql.schema.execution

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.request.VariablesJson
import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.scalar.serializeScalar
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.reflect.KProperty1


@Suppress("UNCHECKED_CAST") // For valid structure there is no risk of ClassCastException
class ParallelRequestExecutor(val schema: DefaultSchema) : RequestExecutor {

    data class Context(val variables: Variables)

    private val argumentsHandler = ArgumentsHandler(schema)

    private val functionHandler = FunctionInvoker(argumentsHandler)

    private val jsonNodeFactory = JsonNodeFactory.instance

    private val dispatcher = schema.configuration.coroutineDispatcher

    private val objectWriter = schema.configuration.objectMapper.writer().let {
        if(schema.configuration.useDefaultPrettyPrinter){
            it.withDefaultPrettyPrinter()
        } else {
            it
        }
    }

    override fun execute(plan : ExecutionPlan, variables: VariablesJson) : String = runBlocking {
        val root = jsonNodeFactory.objectNode()
        val data = root.putObject("data")
        val channel = Channel<Pair<Execution, JsonNode>>()
        val jobs = plan
                .map { execution ->
                    launch(dispatcher) {
                        try {
                            val writeOperation = writeOperation(
                                    ctx = Context(Variables(schema, variables, execution.variables)),
                                    node = execution,
                                    operation = execution.operationNode
                            )
                            channel.send(execution to writeOperation)
                        } catch (e: Exception) {
                            channel.close(e)
                        }
                    }
                }
                .toList()

        //intermediate data structure necessary to preserve ordering
        val resultMap = mutableMapOf<Execution, JsonNode>()
        repeat(plan.size) {
            try {
                val (execution, jsonNode) = channel.receive()
                resultMap.put(execution, jsonNode)
            } catch(e : Exception){
                jobs.forEach{ it.cancel() }
                throw e
            }
        }
        channel.close()

        for(operation in plan){
            data.set(operation.aliasOrKey, resultMap[operation])
        }

        objectWriter.writeValueAsString(root)
    }

    private fun <T>writeOperation(ctx: Context, node: Execution.Node, operation: SchemaNode.Operation<T>) : JsonNode {
        val operationResult : T? = functionHandler.invokeFunWrapper(
                funName = operation.kqlOperation.name,
                functionWrapper = operation.kqlOperation,
                receiver = null,
                args = node.arguments,
                variables = ctx.variables
        )
        return createNode(ctx, operationResult, node, operation.returnType)
    }

    private fun <T> createUnionOperationNode(ctx: Context, parent: T, node: Execution.Union, unionProperty: SchemaNode.UnionProperty): JsonNode {
        val operationResult : Any? = functionHandler.invokeFunWrapper(
                funName = unionProperty.kqlProperty.name,
                functionWrapper = unionProperty.kqlProperty,
                receiver = parent,
                args = node.arguments,
                variables = ctx.variables
        )

        val returnType = unionProperty.returnTypes.find {
            (it.kqlType as KQLType.Object<*>).kClass.isInstance(operationResult)
        } ?: throw ExecutionException(
                "Unexpected type of union property value, expected one of : ${unionProperty.kqlProperty.union.members}." +
                        " value was $operationResult"
        )

        return createNode(ctx, operationResult, node, returnType)
    }

    private fun <T> createNode(ctx: Context, value : T?,
                               node: Execution.Node,
                               returnType: SchemaNode.ReturnType,
                               isCollectionElement : Boolean = false) : JsonNode {
        return when {
            value == null -> createNullNode(node, returnType, isCollectionElement)

            //check value, not returnType, because this method can be invoked with element value
            value is Collection<*> -> {
                if(returnType.isCollection){
                    val arrayNode = jsonNodeFactory.arrayNode(value.size)
                    value.forEach { element -> arrayNode.add(createNode(ctx, element, node, returnType, true)) }
                    arrayNode
                } else {
                    throw ExecutionException("Invalid collection value for non collection property")
                }
            }
            value is String -> jsonNodeFactory.textNode(value)
            value is Int -> jsonNodeFactory.numberNode(value)
            value is Float -> jsonNodeFactory.numberNode(value)
            value is Double -> jsonNodeFactory.numberNode(value)
            value is Boolean -> jsonNodeFactory.booleanNode(value)
            //big decimal etc?

            node.children.isNotEmpty() -> createObjectNode(ctx, value, node, returnType)
            node is Execution.Union -> {
                createObjectNode(ctx, value, node.memberExecution(returnType), returnType)
            }
            else -> createSimpleValueNode(returnType, value)
        }
    }

    private fun <T> createSimpleValueNode(returnType: SchemaNode.ReturnType, value: T) : JsonNode {
        val kqlType = returnType.kqlType
        return when (kqlType) {
            is KQLType.Scalar<*> -> {
                serializeScalar(jsonNodeFactory, kqlType, value)
            }
            is KQLType.Enumeration<*> -> {
                jsonNodeFactory.textNode(value.toString())
            }
            is KQLType.Object<*> -> throw ExecutionException("Cannot handle object return type, schema structure exception")
            else -> throw ExecutionException("Invalid KQLType:  $kqlType")
        }
    }

    private fun createNullNode(node: Execution.Node, returnType: SchemaNode.ReturnType, collectionElement: Boolean): NullNode {
        val isNullable = if(collectionElement) returnType.areEntriesNullable else returnType.isNullable
        if (isNullable) {
            return jsonNodeFactory.nullNode()
        } else {
            throw ExecutionException("null result for non-nullable operation ${node.schemaNode}")
        }
    }

    private fun <T> createObjectNode(ctx: Context, value : T, node: Execution.Node, type: SchemaNode.Type): ObjectNode {
        val objectNode = jsonNodeFactory.objectNode()
        for(child in node.children){
            if(child is Execution.Fragment){
                objectNode.setAll(handleFragment(ctx, value, child))
            } else {
                val (key, jsonNode) = handleProperty(ctx, value, child, type)
                objectNode.set(key, jsonNode)
            }
        }
        return objectNode
    }

    private fun <T> handleProperty(ctx: Context, value: T, child: Execution, type: SchemaNode.Type): Pair<String, JsonNode?> {
        when (child) {
        //Union is subclass of Node so check it first
            is Execution.Union -> {
                val property = type.unionProperties[child.key]
                        ?: throw IllegalStateException("Execution unit ${child.key} is not contained by operation return type")
                return child.aliasOrKey to createUnionOperationNode(ctx, value, child, property)
            }
            is Execution.Node -> {
                val property = type.properties[child.key]
                        ?: throw IllegalStateException("Execution unit ${child.key} is not contained by operation return type")
                return child.aliasOrKey to createPropertyNode(ctx, value, child, property)
            }
            else -> {
                throw UnsupportedOperationException("Handling containers is not implemented yet")
            }
        }
    }

    private fun <T> handleFragment(ctx: Context, value: T, container: Execution.Fragment): Map<String, JsonNode?> {
        val schemaNode = container.condition.schemaNode
        val include = determineInclude(ctx, container.directives)

        if(include){
            if(schemaNode.kqlType is KQLType.Object<*>){
                if(schemaNode.kqlType.kClass.isInstance(value)){
                    return container.elements.map { handleProperty(ctx, value, it, schemaNode)}.toMap()
                }
            } else {
                throw IllegalStateException("fragments can be specified on object types, interfaces, and unions")
            }
        }
        //not included, or type condition is not matched
        return emptyMap()
    }

    private fun <T> createPropertyNode(ctx: Context, parentValue: T, node: Execution.Node, property: SchemaNode.Property) : JsonNode? {
        val include = determineInclude(ctx, node.directives)

        if(include){
            val kqlProperty = property.kqlProperty
            when(kqlProperty){
                is KQLProperty.Kotlin<*,*> -> {
                    kqlProperty.kProperty as KProperty1<T, *>
                    val rawValue = kqlProperty.kProperty.get(parentValue)
                    val value : Any?
                    if(property.transformation != null){
                        value = functionHandler.invokeFunWrapper(
                                funName = kqlProperty.name,
                                functionWrapper = property.transformation,
                                receiver = rawValue,
                                args = node.arguments,
                                variables = ctx.variables
                        )
                    } else {
                        value = rawValue
                    }
                    return createNode(ctx, value, node, property.returnType)
                }
                is KQLProperty.Function<*> -> {
                    return handleFunctionProperty(ctx, parentValue, node, property, kqlProperty)
                }
                else -> {
                    throw Exception("Unexpected kql property type: $kqlProperty, should be KQLProperty.Kotlin or KQLProperty.Function")
                }
            }
        } else {
            return null
        }
    }

    fun <T>handleFunctionProperty(ctx: Context, parentValue: T, node: Execution.Node, property: SchemaNode.Property, kqlProperty: KQLProperty.Function<*>) : JsonNode {
        val result = functionHandler.invokeFunWrapper(
                funName = kqlProperty.name,
                functionWrapper = kqlProperty,
                receiver = parentValue,
                args = node.arguments,
                variables = ctx.variables
        )
        return createNode(ctx, result, node, property.returnType)
    }

    private fun determineInclude(ctx: Context, directives: Map<Directive, Arguments?>?): Boolean {
        return directives?.map { (directive, arguments) ->
            functionHandler.invokeFunWrapper(
                    funName = directive.name,
                    functionWrapper = directive.execution,
                    receiver = null,
                    args = arguments,
                    variables = ctx.variables
            )?.include ?: throw ExecutionException("Illegal directive implementation returning null result")
        }?.reduce { acc, b -> acc && b } ?: true
    }
}