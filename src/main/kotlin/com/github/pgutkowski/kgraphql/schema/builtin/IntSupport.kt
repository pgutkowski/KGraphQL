package com.github.pgutkowski.kgraphql.schema.builtin

import com.github.pgutkowski.kgraphql.schema.ScalarSupport


class IntSupport : ScalarSupport<Int> {

    private val intRegex = Regex("\\d+")

    override fun serialize(input: String): Int = input.toInt()

    override fun deserialize(input: Int): String = input.toString()

    override fun validate(input: String): Boolean = input.matches(intRegex)
}