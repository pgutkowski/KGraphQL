package com.github.pgutkowski.kgraphql.schema.introspection


data class __InputValue(
        val type: __Type,
        val defaultValue: String?,
        override val name: String,
        override val description: String?
) : __Described