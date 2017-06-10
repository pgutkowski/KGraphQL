package com.github.pgutkowski.kgraphql.schema.dsl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.configuration.SchemaConfiguration


class SchemaConfigurationDSL {
    var useDefaultPrettyPrinter: Boolean = false
    var objectMapper: ObjectMapper = jacksonObjectMapper()
    var executeParalleled : Boolean = true

    internal fun update(block : SchemaConfigurationDSL.() -> Unit) = block()

    internal fun build() : SchemaConfiguration {
        return SchemaConfiguration(
                useDefaultPrettyPrinter, objectMapper, executeParalleled
        )
    }
}