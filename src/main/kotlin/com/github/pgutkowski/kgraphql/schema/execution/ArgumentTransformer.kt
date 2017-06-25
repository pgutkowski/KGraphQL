package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.isLiteral
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.scalar.deserializeScalar
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure


open class ArgumentTransformer(val schema : DefaultSchema) {

    private val enumsByType = schema.definition.enums.associate { it.kClass.createType() to it }

    private val scalarsByType = schema.definition.scalars.associate { it.kClass.createType() to it }

    fun transformValue(type: KType, value: String, variables: Variables) : Any? {
        return when {
            value.startsWith("$") -> {
                variables.get (
                        type.jvmErasure, type, value, {value, type -> transformValue(type.starProjectedType, value, variables) }
                )
            }
            value == "null" && type.isMarkedNullable -> null
            value == "null" && !type.isMarkedNullable -> {
                throw RequestException("argument '$value' is not valid value of type ${schema.inputTypeByKType(type)?.name}")
            }
            else -> {
                return transformString(value, type.withNullability(false))
            }
        }
    }

    private fun transformString(value: String, lookupType: KType): Any {

        fun throwInvalidEnumValue(enumType : KQLType.Enumeration<*>){
            throw RequestException(
                    "Invalid enum ${schema.inputTypeByKType(lookupType)?.name} value. Expected one of ${enumType.values}"
            )
        }

        enumsByType[lookupType]?.let { enumType ->
            if(value.isLiteral()) {
                throw RequestException("String literal '$value' is invalid value for enum type ${enumType.name}")
            }
            return enumType.values.find { it.name == value }?.value ?: throwInvalidEnumValue(enumType)
        } ?: scalarsByType[lookupType]?.let { scalarType ->
            return deserializeScalar(scalarType, value)
        } ?: throw RequestException("Invalid argument value '$value' for type ${schema.inputTypeByKType(lookupType)?.name}")
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
}