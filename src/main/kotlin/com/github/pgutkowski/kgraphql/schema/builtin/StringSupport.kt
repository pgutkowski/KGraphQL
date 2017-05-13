package com.github.pgutkowski.kgraphql.schema.builtin

import com.github.pgutkowski.kgraphql.schema.ScalarSupport


class StringSupport : ScalarSupport<String> {
    override fun serialize(input: String): String = input
    override fun deserialize(input: String): String = input
    override fun validate(input: String): Boolean = true
}