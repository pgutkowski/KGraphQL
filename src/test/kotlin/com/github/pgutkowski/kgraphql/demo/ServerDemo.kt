package com.github.pgutkowski.kgraphql.demo

import com.github.pgutkowski.kgraphql.integration.QueryTest
import com.github.pgutkowski.kgraphql.server.NettyServer

/**
 * Demo application showing of tested schema, by default runs on localhost:8080
 */
fun main(args : Array<String>) {
    val schema = QueryTest().testedSchema
    NettyServer.run(schema, 8080)
}