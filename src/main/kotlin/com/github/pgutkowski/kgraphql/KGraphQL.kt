package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.SchemaBuilder
import com.github.pgutkowski.kgraphql.server.NettyServer


class KGraphQL {
    companion object {
        fun newSchema() : SchemaBuilder {
            return SchemaBuilder()
        }

        fun setupServer(schema: Schema) = NettyServer.run(schema)
    }
}