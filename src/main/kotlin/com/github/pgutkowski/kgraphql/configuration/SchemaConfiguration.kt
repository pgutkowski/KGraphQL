package com.github.pgutkowski.kgraphql.configuration

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper

data class SchemaConfiguration (
        val prettyPrinter: PrettyPrinter?,
        val useDefaultPrettyPrinter: Boolean,
        val objectMapper: ObjectMapper
)
