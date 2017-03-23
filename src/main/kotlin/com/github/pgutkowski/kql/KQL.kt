package com.github.pgutkowski.kql

import com.github.pgutkowski.kql.schema.SchemaBuilder
import com.github.pgutkowski.kql.schema.impl.DefaultSchemaBuilder


class KQL {
    companion object {
        fun newSchema() : SchemaBuilder {
            return DefaultSchemaBuilder()
        }
    }
}