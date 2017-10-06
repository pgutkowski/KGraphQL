package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.dsl.SchemaBuilder


class KGraphQL {
    companion object {
        fun schema(init : SchemaBuilder<Unit>.() -> Unit) = SchemaBuilder(init).build()
    }
}