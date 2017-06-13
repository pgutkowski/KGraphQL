package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.dropQuotes
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure


open class ArgumentTransformer(val schema : DefaultSchema) {

    private val enumsByType = schema.model.enums.associate { it.kClass.createType() to it }

    private val scalarsByType = schema.model.scalars.associate { it.kClass.createType() to it }

    fun transformValue(type: KType, value: String, variables: Variables) : Any? {
        return when {
            value.startsWith("$") -> {
                variables.get (
                        type.jvmErasure, value, {value, type -> transformValue(type.starProjectedType, value, variables) }
                )
            }
            value == "null" && type.isMarkedNullable -> null
            value == "null" && !type.isMarkedNullable -> {
                throw SyntaxException("argument '$value' is not valid value of type ${schema.typeByKType(type)?.name}")
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

    fun transformCollectionElementValue(collectionParameter: KParameter, value: String, variables: Variables): Any? {
        assert(collectionParameter.type.jvmErasure == List::class)
        val elementType = collectionParameter.type.arguments.firstOrNull()?.type
                ?: throw ExecutionException("Unable to handle value of element of collection without type")

        return transformValue(elementType, value, variables)
    }

    fun transformPropertyValue(parameter: KParameter, value: String, variables: Variables): Any? {
        return transformValue(parameter.type, value, variables)
    }

    private fun <T : Any>transformScalar(support : KQLType.Scalar<T>, value : String): T {
        try {
            return support.scalarSupport.serialize(value)
        } catch (e: Exception){
            throw SyntaxException("argument '$value' is not valid value of type ${support.name}", e)
        }
    }
}