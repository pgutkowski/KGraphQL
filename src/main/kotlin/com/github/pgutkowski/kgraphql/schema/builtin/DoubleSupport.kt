package com.github.pgutkowski.kgraphql.schema.builtin

import com.github.pgutkowski.kgraphql.schema.ScalarSupport


class DoubleSupport : ScalarSupport<Double> {

    private val floatRegex = Regex("^([+-]?(\\d+\\.)?\\d+)$")

    override fun serialize(input: String): Double = input.toDouble()

    override fun deserialize(input: Double): String = input.toString()

    override fun validate(input: String): Boolean = input.matches(floatRegex)
}