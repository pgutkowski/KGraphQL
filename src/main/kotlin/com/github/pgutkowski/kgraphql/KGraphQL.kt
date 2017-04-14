package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.SchemaBuilder


class KGraphQL {
    companion object {
        fun newSchema() : SchemaBuilder {
            return SchemaBuilder()
        }
    }
}