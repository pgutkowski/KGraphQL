package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.schema.impl.SchemaDescriptor


interface Schema {

    val descriptor : SchemaDescriptor

    fun handleRequest(request: String, variables: String?) : String
}