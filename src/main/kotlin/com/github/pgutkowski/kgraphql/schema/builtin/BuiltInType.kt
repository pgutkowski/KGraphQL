package com.github.pgutkowski.kgraphql.schema.builtin

import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.typeName


class BuiltInType {
    companion object {
        //TODO: add descriptions
        val STRING = KQLType.Scalar(String::class.typeName(), String::class, StringSupport(), null)
        val INT = KQLType.Scalar(Int::class.typeName(), Int::class, IntSupport(), null)
        val DOUBLE = KQLType.Scalar(Double::class.typeName(), Double::class, DoubleSupport(), null)
        val FLOAT = KQLType.Scalar(Float::class.typeName(), Float::class, FloatSupport(), null)
        val BOOLEAN = KQLType.Scalar(Boolean::class.typeName(), Boolean::class, BooleanSupport(), null)
    }
}