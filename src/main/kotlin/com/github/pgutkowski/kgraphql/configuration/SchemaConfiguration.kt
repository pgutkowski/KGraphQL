package com.github.pgutkowski.kgraphql.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.experimental.CoroutineDispatcher

data class SchemaConfiguration (
        val useDefaultPrettyPrinter: Boolean,
        val useCachingDocumentParser: Boolean,
        val documentParserCacheMaximumSize : Long,
        val objectMapper: ObjectMapper,
        val coroutineDispatcher: CoroutineDispatcher
)
