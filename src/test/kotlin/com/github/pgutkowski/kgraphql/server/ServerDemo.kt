package com.github.pgutkowski.kgraphql.server

import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.integration.QueryTest


fun main(args : Array<String>) {
    val schema = QueryTest().testedSchema
    KGraphQL.setupServer(schema)
}