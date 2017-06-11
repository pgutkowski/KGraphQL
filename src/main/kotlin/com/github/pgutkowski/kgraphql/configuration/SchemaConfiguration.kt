package com.github.pgutkowski.kgraphql.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.experimental.CoroutineDispatcher

data class SchemaConfiguration (
        val useDefaultPrettyPrinter: Boolean,
        val objectMapper: ObjectMapper,
        val coroutineDispatcher: CoroutineDispatcher
)
