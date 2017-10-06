package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.Context
import com.github.pgutkowski.kgraphql.schema.introspection.__Schema


interface Schema : __Schema {
    fun execute(request: String, variables: String?, context: Context = Context(emptyMap())) : String

    fun execute(request: String, context: Context = Context(emptyMap())) = execute(request, null, context)
}