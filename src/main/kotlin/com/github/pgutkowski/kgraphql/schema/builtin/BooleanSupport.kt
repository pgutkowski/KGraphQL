package com.github.pgutkowski.kgraphql.schema.builtin

import com.github.pgutkowski.kgraphql.schema.ScalarSupport


class BooleanSupport : ScalarSupport<Boolean> {
    override fun serialize(input: String): Boolean = input.toBoolean()

    override fun deserialize(input: Boolean): String = input.toString()

    override fun validate(input: String): Boolean {
        return input.equals("true", true) || input.equals("false", true)
    }
}