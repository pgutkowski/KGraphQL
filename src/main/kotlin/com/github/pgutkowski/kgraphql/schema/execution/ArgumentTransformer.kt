package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.isLiteral
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.scalar.deserializeScalar
import com.github.pgutkowski.kgraphql.schema.structure2.InputValue
import com.github.pgutkowski.kgraphql.schema.structure2.Type
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure


open class ArgumentTransformer(val schema : DefaultSchema<*>) {

    fun transformValue(type: Type, value: String, variables: Variables) : Any? {
        val kType = type.toKType()

        return when {
            value.startsWith("$") -> {
                variables.get (
                        kType.jvmErasure, kType, value, { subValue -> transformValue(type, subValue, variables) }
                )
            }
            value == "null" && type.isNullable() -> null
            value == "null" && type.isNotNullable() -> {
                throw RequestException("argument '$value' is not valid value of type ${type.unwrapped().name}")
            }
            else -> {
                return transformString(value, kType)
            }
        }

    }

    private fun transformString(value: String, kType: KType): Any {

        val kClass = kType.jvmErasure

        fun throwInvalidEnumValue(enumType : Type.Enum<*>){
            throw RequestException(
                    "Invalid enum ${schema.model.enums[kClass]?.name} value. Expected one of ${enumType.values}"
            )
        }

        schema.model.enums[kClass]?.let { enumType ->
            if(value.isLiteral()) {
                throw RequestException("String literal '$value' is invalid value for enum type ${enumType.name}")
            }
            return enumType.values.find { it.name == value }?.value ?: throwInvalidEnumValue(enumType)
        } ?: schema.model.scalars[kClass]?.let { scalarType ->
            return deserializeScalar(scalarType, value)
        } ?: throw RequestException("Invalid argument value '$value' for type ${schema.model.inputTypes[kClass]?.name}")
    }

    fun transformCollectionElementValue(inputValue: InputValue<*>, value: String, variables: Variables): Any? {
        assert(inputValue.type.isList())
        val elementType = inputValue.type.unwrapList().ofType as Type?
                ?: throw ExecutionException("Unable to handle value of element of collection without type")

        return transformValue(elementType, value, variables)
    }

    fun transformPropertyValue(parameter: InputValue<*>, value: String, variables: Variables): Any? {
        return transformValue(parameter.type, value, variables)
    }
}