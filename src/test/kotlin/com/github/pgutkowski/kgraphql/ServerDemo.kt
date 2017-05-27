package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.integration.QueryTest


fun main(args : Array<String>) {
    val schema = QueryTest().testedSchema
    KGraphQL.setupServer(schema)
}