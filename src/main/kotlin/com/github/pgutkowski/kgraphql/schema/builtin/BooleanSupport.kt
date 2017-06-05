package com.github.pgutkowski.kgraphql.schema.builtin

import com.github.pgutkowski.kgraphql.schema.ScalarSupport


class BooleanSupport : ScalarSupport<Boolean> {
    override fun serialize(input: String): Boolean {
        return when {
            input.equals("true", true) -> true
            input.equals("false", true) -> false
            else -> throw IllegalArgumentException("$input does not represent valid Boolean value")
        }
    }

    override fun deserialize(input: Boolean): String = input.toString()

    override fun validate(input: String): Boolean {
        return input.equals("true", true) || input.equals("false", true)
    }
}