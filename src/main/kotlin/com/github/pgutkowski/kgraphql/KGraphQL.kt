package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.dsl.SchemaBuilder
import com.github.pgutkowski.kgraphql.server.NettyServer
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure


class KGraphQL {
    companion object {
        fun schema(init : SchemaBuilder.() -> Unit) : Schema {
            return SchemaBuilder(init).build()
        }

        fun setupServer(schema: Schema, port: Int = 8080) = NettyServer.run(schema, port)
    }
}