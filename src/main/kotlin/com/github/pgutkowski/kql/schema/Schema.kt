package com.github.pgutkowski.kql.schema

import com.github.pgutkowski.kql.query.Result


interface Schema {
    fun handleQuery(query: String) : Result
}