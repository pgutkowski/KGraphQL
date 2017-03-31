package com.github.pgutkowski.kql.schema


interface Schema {
    fun handleRequest(request: String) : String
}