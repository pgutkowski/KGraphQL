package com.github.pgutkowski.kgraphql.schema.dsl

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.configuration.SchemaConfiguration


class SchemaConfigurationDSL {
    var prettyPrinter: PrettyPrinter? = null
    var useDefaultPrettyPrinter: Boolean = false
    var objectMapper: ObjectMapper = jacksonObjectMapper()

    internal fun update(block : SchemaConfigurationDSL.() -> Unit) = block()

    internal fun build() : SchemaConfiguration {
        if(useDefaultPrettyPrinter && prettyPrinter != null){
            throw IllegalArgumentException(
                    "Cannot use both default and configured prettyPrinter. " +
                    "Please declare either of them, not both"
            )
        }
        return SchemaConfiguration(
                prettyPrinter, useDefaultPrettyPrinter, objectMapper
        )
    }
}