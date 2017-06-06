package com.github.pgutkowski.kgraphql.schema.builtin

import com.github.pgutkowski.kgraphql.schema.ScalarSupport


class FloatSupport : ScalarSupport<Float>{

    private val floatRegex = Regex("^([+-]?(\\d+\\.)?\\d+)$")

    override fun serialize(input: String): Float = input.toFloat()

    override fun deserialize(input: Float): String = input.toDouble().toString()

    override fun validate(input: String): Boolean = input.matches(floatRegex)
}