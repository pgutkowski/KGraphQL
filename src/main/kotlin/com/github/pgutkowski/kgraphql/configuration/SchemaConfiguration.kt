package com.github.pgutkowski.kgraphql.configuration

import com.fasterxml.jackson.databind.ObjectMapper

data class SchemaConfiguration (
        val useDefaultPrettyPrinter: Boolean,
        val objectMapper: ObjectMapper,
        val executeParalleled: Boolean
)
