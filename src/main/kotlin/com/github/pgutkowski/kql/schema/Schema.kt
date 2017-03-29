package com.github.pgutkowski.kql.schema

import com.github.pgutkowski.kql.result.Result


interface Schema {
    fun handleRequest(request: String) : Result

    fun handleRequestAsJson(request: String) : String
}