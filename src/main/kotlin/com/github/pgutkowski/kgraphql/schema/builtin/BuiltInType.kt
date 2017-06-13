package com.github.pgutkowski.kgraphql.schema.builtin

import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.defaultKQLTypeName


private val STRING_DESCRIPTION = "The String scalar type represents textual data, represented as UTF‐8 character sequences"
private val INT_DESCRIPTION = "The Int scalar type represents a signed 32‐bit numeric non‐fractional value"
private val FLOAT_DESCRIPTION = "The Float scalar type represents signed double‐precision fractional values as specified by IEEE 754"
private val BOOLEAN_DESCRIPTION = "The Boolean scalar type represents true or false"

class BuiltInType {
    companion object {
        val STRING = KQLType.Scalar(String::class.defaultKQLTypeName(), String::class, StringSupport(), STRING_DESCRIPTION)
        val INT = KQLType.Scalar(Int::class.defaultKQLTypeName(), Int::class, IntSupport(), INT_DESCRIPTION)
        //GraphQL does not differ float and double, treat double like float
        val DOUBLE = KQLType.Scalar(Float::class.defaultKQLTypeName(), Double::class, DoubleSupport(), FLOAT_DESCRIPTION)
        val FLOAT = KQLType.Scalar(Float::class.defaultKQLTypeName(), Float::class, FloatSupport(), FLOAT_DESCRIPTION)
        val BOOLEAN = KQLType.Scalar(Boolean::class.defaultKQLTypeName(), Boolean::class, BooleanSupport(), BOOLEAN_DESCRIPTION)
    }
}