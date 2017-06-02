package com.github.pgutkowski.kgraphql.demo

import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.integration.QueryTest

/**
 * Demo application showing of tested schema, by default runs on localhost:8080
 */
fun main(args : Array<String>) {
    val schema = QueryTest().testedSchema
    KGraphQL.setupServer(schema)
}