package com.github.pgutkowski.kgraphql.schema.execution

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.request.VariablesJson
import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode
import java.io.StringWriter
import kotlin.reflect.KProperty1


@Suppress("UNCHECKED_CAST") // For valid structure there is no risk of ClassCastException
class RequestExecutor(val schema: DefaultSchema) {

    companion object {
        private val jsonFactory = JsonFactory()
    }

    data class Context(val generator: JsonGenerator, val variables: Variables)

    private val argumentsHandler = ArgumentsHandler(schema.model)

    private val functionHandler = FunctionInvoker(argumentsHandler)

    fun execute(plan : ExecutionPlan, variables: VariablesJson) : String {

        StringWriter().use { stringWriter ->
            jsonFactory.createGenerator(stringWriter).use { gen ->
//                gen.useDefaultPrettyPrinter()
                gen.writeStartObject()
                gen.writeFieldName("data")
                gen.writeStartObject()
                for(child in plan){
                    writeOperation(Context(gen, Variables(variables, child.variables)), child, child.operationNode)
                }
                gen.writeEndObject()
                gen.writeEndObject()

                gen.flush()
                gen.close()
            }
            stringWriter.flush()
            return stringWriter.buffer.toString()
        }
    }

    private fun <T>writeOperation(ctx: Context, node: Execution.Node, operation: SchemaNode.Operation<T>){
        val operationResult : T? = functionHandler.invokeFunWrapper(
                funName = operation.kqlOperation.name,
                functionWrapper = operation.kqlOperation,
                receiver = null,
                args = node.arguments,
                variables = ctx.variables
        )
        ctx.generator.writeFieldName(node.aliasOrKey)

        writeValue(ctx, operationResult, node, operation.returnType)
    }

    private fun <T>writeUnionOperation(ctx: Context, parent: T, node: Execution.Union, unionProperty: SchemaNode.UnionProperty){
        val operationResult : Any? = functionHandler.invokeFunWrapper(
                funName = unionProperty.kqlProperty.name,
                functionWrapper = unionProperty.kqlProperty,
                receiver = parent,
                args = node.arguments,
                variables = ctx.variables
        )

        ctx.generator.writeFieldName(node.aliasOrKey)

        val returnType = unionProperty.returnTypes.find {
            (it.kqlType as KQLType.Object<*>).kClass.isInstance(operationResult)
        } ?: throw ExecutionException(
                "Unexpected type of union property value, expected one of : ${unionProperty.kqlProperty.union.members}." +
                        " value was $operationResult"
        )

        writeValue(ctx, operationResult, node, returnType)
    }

    private fun <T>writeValue(ctx: Context, value : T?, node: Execution.Node, returnType: SchemaNode.ReturnType){
        when {
            value == null -> writeNullValue(ctx, node, returnType)

            //check value, not returnType, because this method can be invoked with element value
            value is Collection<*> -> {
                if(returnType.isCollection){
                    ctx.generator.writeStartArray(value.size)
                    value.forEach { writeValue(ctx, it, node, returnType) }
                    ctx.generator.writeEndArray()
                } else {
                    throw ExecutionException("Invalid collection value for non collection property")
                }
            }
            value is String -> ctx.generator.writeString(value)
            value is Int -> ctx.generator.writeNumber(value)
            value is Float -> ctx.generator.writeNumber(value)
            value is Double -> ctx.generator.writeNumber(value)
            value is Boolean -> ctx.generator.writeBoolean(value)
            //big decimal etc?

            node.children.isNotEmpty() -> writeObject(ctx, value, node, returnType)
            node is Execution.Union -> {
                writeObject(ctx, value, node.memberExecution(returnType), returnType)
            }
            else -> writeSimpleValue(ctx, returnType, value)
        }
    }

    private fun <T> writeSimpleValue(ctx: Context, returnType: SchemaNode.ReturnType, value: T) {
        val kqlType = returnType.kqlType
        when (kqlType) {
            is KQLType.Object<*> -> throw ExecutionException("Cannot handle object return type, schema structure exception")
            is KQLType.Scalar<*> -> {
                ctx.generator.writeString((kqlType.scalarSupport as ScalarSupport<T>).deserialize(value))
            }
            is KQLType.Enumeration<*> -> {
                ctx.generator.writeString(value.toString())
            }
        }
    }

    private fun writeNullValue(ctx: Context, node: Execution.Node, returnType: SchemaNode.ReturnType) {
        if (returnType.isNullable) {
            ctx.generator.writeNull()
        } else {
            throw ExecutionException("null result for non-nullable operation ${node.schemaNode}")
        }
    }

    private fun <T> writeObject(ctx: Context, value : T, node: Execution.Node, type: SchemaNode.Type){
        ctx.generator.writeStartObject()
        for(child in node.children){
            handleProperty(ctx, value, child, type)
        }
        ctx.generator.writeEndObject()
    }

    private fun <T> handleProperty(ctx: Context, value: T, child: Execution, type: SchemaNode.Type) {
        when (child) {
        //Union is subclass of Node so check it first
            is Execution.Union -> {
                val property = type.unionProperties[child.key]
                        ?: throw IllegalStateException("Execution unit ${child.key} is not contained by operation return type")
                writeUnionOperation(ctx, value, child, property)
            }
            is Execution.Node -> {
                val property = type.properties[child.key]
                        ?: throw IllegalStateException("Execution unit ${child.key} is not contained by operation return type")
                writeProperty(ctx, value, child, property)
            }
            is Execution.Fragment -> {
                handleFragment(ctx, value, child)
            }
            else -> {
                throw UnsupportedOperationException("Handling containers is not implemented yet")
            }
        }
    }

    private fun <T> handleFragment(ctx: Context, value: T, container: Execution.Fragment) {
        val schemaNode = container.condition.schemaNode
        if(schemaNode.kqlType is KQLType.Object<*>){
            if(schemaNode.kqlType.kClass.isInstance(value)){
                container.elements.forEach{ handleProperty(ctx, value, it, schemaNode)}
            }
        } else {
            throw IllegalStateException("fragments can be specified on object types, interfaces, and unions")
        }
    }

    private fun <T> writeProperty(ctx: Context, parentValue: T, node: Execution.Node, property: SchemaNode.Property) {
        val include = node.directives?.map { (directive, arguments) ->
            functionHandler.invokeFunWrapper(
                    funName = directive.name,
                    functionWrapper = directive.execution,
                    receiver = null,
                    args = arguments,
                    variables = ctx.variables
            )?.include ?: throw ExecutionException("Illegal directive implementation returning null result")
        }?.reduce { acc, b -> acc && b } ?: true

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
                    ctx.generator.writeFieldName(node.aliasOrKey)
                    writeValue(ctx, value, node, property.returnType)
                }
                is KQLProperty.Function<*> -> {
                    val args = argumentsHandler.transformArguments(kqlProperty, node.arguments, ctx.variables)
                    val result = kqlProperty.invoke(parentValue, *args.toTypedArray())
                    ctx.generator.writeFieldName(node.aliasOrKey)
                    writeValue(ctx, result, node, property.returnType)
                }
                else -> {
                    throw Exception("Unexpected kql property type: $kqlProperty, should be KQLProperty.Kotlin or KQLProperty.Function")
                }
            }
        }
    }
}