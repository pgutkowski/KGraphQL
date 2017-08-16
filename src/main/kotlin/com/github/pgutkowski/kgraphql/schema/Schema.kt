package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.schema.introspection.__Schema


interface Schema<Context> : __Schema {
    fun execute(request: String, variables: String? = null, context: Context? = null) : String

    fun execute(request: String, context: Context) : String = execute(request, null, context)
}