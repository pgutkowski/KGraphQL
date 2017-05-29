package com.github.pgutkowski.kgraphql.schema


interface Schema {
    fun execute(request: String, variables: String? = null) : String
}