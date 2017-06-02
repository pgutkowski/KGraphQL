package com.github.pgutkowski.kgraphql.configuration

import com.fasterxml.jackson.core.PrettyPrinter

data class SchemaConfiguration (
        val prettyPrinter: PrettyPrinter,
        val useDefaultPrettyPrinter: Boolean
)
