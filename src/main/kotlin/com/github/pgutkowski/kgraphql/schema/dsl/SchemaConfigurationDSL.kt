package com.github.pgutkowski.kgraphql.schema.dsl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.configuration.SchemaConfiguration
import kotlinx.coroutines.experimental.CommonPool


class SchemaConfigurationDSL {
    var useDefaultPrettyPrinter: Boolean = false
    var useCachingDocumentParser: Boolean = true
    var documentParserCacheMaximumSize : Long = 1000L
    var objectMapper: ObjectMapper = jacksonObjectMapper()
    var coroutineDispatcher = CommonPool

    internal fun update(block : SchemaConfigurationDSL.() -> Unit) = block()

    internal fun build() : SchemaConfiguration = SchemaConfiguration(
            useDefaultPrettyPrinter,
            useCachingDocumentParser,
            documentParserCacheMaximumSize,
            objectMapper,
            coroutineDispatcher
    )
}