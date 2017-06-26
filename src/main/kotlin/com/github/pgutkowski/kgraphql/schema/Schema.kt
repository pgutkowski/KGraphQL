package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.schema.introspection.__Schema


interface Schema : __Schema {
    fun execute(request: String, variables: String? = null) : String
}