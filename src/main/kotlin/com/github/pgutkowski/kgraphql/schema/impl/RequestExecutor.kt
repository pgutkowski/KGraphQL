package com.github.pgutkowski.kgraphql.schema.impl

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.schema.model.KQLObject
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import java.io.StringWriter
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure


@Suppress("UNCHECKED_CAST") // For valid structure there is no risk of ClassCastException
class RequestExecutor(val schema: DefaultSchema) {

    companion object {
        private val jsonFactory = JsonFactory()
    }

    data class Context(val generator: JsonGenerator, val variables: Variables)

    private val enumsByType = schema.enums.associate { it.kClass.createType() to it }

    private val scalarsByType = schema.scalars.associate { it.kClass.createType() to it }

    fun execute(plan : ExecutionPlan, variables: Variables) : String {

        StringWriter().use { stringWriter ->
            jsonFactory.createGenerator(stringWriter).use { gen ->
                gen.writeStartObject()
                gen.writeFieldName("data")
                gen.writeStartObject()
                for(child in plan){
                    writeOperation(Context(gen, variables), child, child.operationNode)
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

    private fun <T>writeOperation(ctx: Context, node: ExecutionNode, operation: SchemaNode.Operation<T>){
        val operationResult : T? = invokeFunWrapper(operation.kqlOperation, node.arguments ?: Arguments(), ctx.variables)
        ctx.generator.writeFieldName(node.aliasOrKey)

        writeValue(ctx, operationResult, node, operation.returnType)
    }

    private fun <T>writeValue(ctx: Context, value : T?, node: ExecutionNode, returnType: SchemaNode.ReturnType){
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

    private fun writeNullValue(ctx: Context, node: ExecutionNode, returnType: SchemaNode.ReturnType) {
        if (returnType.isNullable) {
            ctx.generator.writeNull()
        } else {
            throw ExecutionException("null result for non-nullable operation ${node.schemaNode}")
        }
    }

    private fun <T> writeObject(ctx: Context, value : T, node: ExecutionNode, type: SchemaNode.Type){
        ctx.generator.writeStartObject()
        for(child in node.children){
            val property = type.properties[child.key]
                    ?: throw IllegalStateException("Execution unit ${child.key} is not contained by operation return type")
            writeProperty(ctx, value, child, property)
        }
        ctx.generator.writeEndObject()
    }

    private fun <T> writeProperty(ctx: Context, parentValue: T, node: ExecutionNode, property: SchemaNode.Property) {
        when(property.kqlProperty){
            is KQLProperty.Kotlin<*,*> -> {
                property.kqlProperty.kProperty as KProperty1<T, *>
                val value = property.kqlProperty.kProperty.get(parentValue)
                ctx.generator.writeFieldName(node.aliasOrKey)
                writeValue(ctx, value, node, property.returnType)
            }
            is KQLProperty.Function<*> -> throw Exception()
            is KQLProperty.Union -> throw Exception()
        }
    }

    fun <T>invokeFunWrapper(functionWrapper: FunctionWrapper<T>, args: Arguments, variables: Variables): T? {
        if(functionWrapper.arity() != args.size){

            functionWrapper as KQLObject

            throw SyntaxException(
                    "Mutation ${functionWrapper.name} does support arguments: ${functionWrapper.kFunction.parameters.map { it.name }}. found arguments: ${args.keys}"
            )
        }

        val transformedArgs : MutableList<Any?> = mutableListOf()
        functionWrapper.kFunction.parameters.forEach { parameter ->
            val value = args[parameter.name!!]
            if(value == null){
                if(!parameter.isOptional){
                    throw IllegalArgumentException("${functionWrapper.kFunction.name} argument ${parameter.name} is not optional, value cannot be null")
                } else {
                    transformedArgs.add(null)
                }
            } else if(value is String) {
                val transformedValue: Any? = transformValue(parameter, value, variables)
                transformedArgs.add(transformedValue)
            } else {
                throw SyntaxException("Non string arguments are not supported yet")
            }

        }

        return functionWrapper.invoke(*transformedArgs.toTypedArray())
    }

    private fun transformValue(parameter: KParameter, value: String, variables: Variables): Any? {

        if(value.startsWith("$")){
            return variables.getVariable(parameter.type.jvmErasure, value.substring(1))
        } else {
            val literalValue = value.dropQuotes()
            return when (parameter.type) {
                String::class.starProjectedType -> literalValue
                in enumsByType.keys -> enumsByType[parameter.type]?.values?.find { it.name == literalValue }
                in scalarsByType.keys ->{
                    transformScalar(scalarsByType[parameter.type]!!, literalValue)
                }
                else -> {
                    throw UnsupportedOperationException("Not supported yet")
                }
            }
        }
    }

    private fun <T : Any>transformScalar(support : KQLType.Scalar<T>, value : String): T {
        try {
            return support.scalarSupport.serialize(value)
        } catch (e: Exception){
            throw SyntaxException("argument '$value' is not value of type ${support.name}")
        }
    }

    fun String.dropQuotes() : String = if(startsWith('\"') && endsWith('\"')) drop(1).dropLast(1) else this
}