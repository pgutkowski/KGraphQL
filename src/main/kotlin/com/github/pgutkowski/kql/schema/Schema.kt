package com.github.pgutkowski.kql.schema

import com.github.pgutkowski.kql.request.result.Result


interface Schema {
    fun handleRequest(request: String) : Result
}