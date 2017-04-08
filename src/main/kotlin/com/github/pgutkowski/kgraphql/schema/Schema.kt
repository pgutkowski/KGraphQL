package com.github.pgutkowski.kgraphql.schema


interface Schema {
    fun handleRequest(request: String) : String
}