package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.SchemaBuilder
import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchemaBuilder


class KGraphQL {
    companion object {
        fun newSchema() : SchemaBuilder {
            return DefaultSchemaBuilder()
        }
    }
}