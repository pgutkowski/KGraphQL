package com.github.pgutkowski.kgraphql.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineDispatcher

data class SchemaConfiguration (
        //document parser caching mechanisms
        val useCachingDocumentParser: Boolean,
        val documentParserCacheMaximumSize : Long,
        //jackson features
        val objectMapper: ObjectMapper,
        val useDefaultPrettyPrinter: Boolean,
        //execution
        val coroutineDispatcher: CoroutineDispatcher
)
