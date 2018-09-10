package com.github.pgutkowski.kgraphql.schema.dsl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.configuration.SchemaConfiguration
import kotlinx.coroutines.CommonPool
import kotlinx.coroutines.CoroutineDispatcher


class SchemaConfigurationDSL {
    var useDefaultPrettyPrinter: Boolean = false
    var useCachingDocumentParser: Boolean = true
    var objectMapper: ObjectMapper = jacksonObjectMapper()
    var documentParserCacheMaximumSize : Long = 1000L
    var acceptSingleValueAsArray : Boolean = true
    var coroutineDispatcher: CoroutineDispatcher = CommonPool

    internal fun update(block : SchemaConfigurationDSL.() -> Unit) = block()

    internal fun build() : SchemaConfiguration {
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, acceptSingleValueAsArray)
        return SchemaConfiguration (
                useCachingDocumentParser,
                documentParserCacheMaximumSize,
                objectMapper,
                useDefaultPrettyPrinter,
                coroutineDispatcher
        )
    }
}