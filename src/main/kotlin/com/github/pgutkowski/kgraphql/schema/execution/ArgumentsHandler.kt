package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.*
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.SchemaModel
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure


internal class ArgumentsHandler(val schema : SchemaModel) {

    private val enumsByType = schema.enums.associate { it.kClass.createType() to it }

    private val scalarsByType = schema.scalars.associate { it.kClass.createType() to it }

    fun <T>transformArguments (
            functionWrapper: FunctionWrapper<T>,
            args: Arguments?,
            variables: Variables
    ) : List<Any?>{
        val parameters = functionWrapper.valueParameters()

        return parameters.map { parameter ->
            val value = args?.get(parameter.name)

            when {
                value == null && parameter.isNullable() -> null
                value == null && parameter.isNotNullable() -> {
                    throw IllegalArgumentException("${functionWrapper.kFunction.name} argument ${parameter.name} is not optional, value cannot be null")
                }
                value is String -> transformPropertyValue(parameter, value, variables)
                value is List<*> && parameter.type.jvmErasure == List::class -> {
                    value.map { element ->
                        if(element is String){
                            transformCollectionElementValue(parameter, element, variables)
                        } else {
                            throw ExecutionException("Unexpected non-string list element")
                        }
                    }
                }
                value is List<*> && parameter.type.jvmErasure != List::class -> {
                    throw SyntaxException("Invalid list value passed to non-list argument")
                }
                else -> throw SyntaxException("Non string arguments are not supported yet")
            }
        }
    }

    private fun transformCollectionElementValue(collectionParameter: KParameter, value: String, variables: Variables): Any? {
        assert(collectionParameter.type.jvmErasure == List::class)
        val elementType = collectionParameter.type.arguments.firstOrNull()?.type
                ?: throw ExecutionException("Unable to handle value of element of collection without type")

        return transformValue(elementType, value, variables)
    }

    private fun transformPropertyValue(parameter: KParameter, value: String, variables: Variables): Any? {
        return transformValue(parameter.type, value, variables)
    }

    private fun transformValue(type: KType, value: String, variables: Variables) : Any? {
        return when {
            value.startsWith("$") -> variables.get(type.jvmErasure, value.substring(1))
            value == "null" && type.isMarkedNullable -> null
            value == "null" && !type.isMarkedNullable -> {
                throw SyntaxException("argument '$value' is not valid value of type ${type.typeName()}")
            }
            else -> {
                val literalValue = value.dropQuotes()
                //drop nullability on lookup type
                val kType = type.withNullability(false)
                return when (kType) {
                    String::class.starProjectedType -> literalValue
                    in enumsByType.keys -> enumsByType[kType]?.values?.find { it.name == literalValue }
                    in scalarsByType.keys -> {
                        transformScalar(scalarsByType[kType]!!, literalValue)
                    }
                    else -> {
                        throw UnsupportedOperationException("Not supported yet")
                    }
                }
            }
        }
    }

    private fun <T : Any>transformScalar(support : KQLType.Scalar<T>, value : String): T {
        try {
            return support.scalarSupport.serialize(value)
        } catch (e: Exception){
            throw SyntaxException("argument '$value' is not value of type ${support.name}", e)
        }
    }
}