package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.assertError
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import org.junit.jupiter.api.Test

/**
 * GraphQL documents are expressed as a sequence of Unicode characters. However, with few exceptions,
 * most of GraphQL is expressed only in the original non‐control ASCII range so as to be as widely compatible
 * with as many existing tools, languages, and serialization formats as possible and avoid display issues
 * in text editors and source control.
 */
@Specification("2.1")
class SourceText {

    val schema = defaultSchema {
        query {
            name = "fizz"
            resolver({ -> "buzz"})
        }
    }

    @Test
    fun `invalid unicode character`() {
        val map = deserialize(schema.handleRequest("\u0003", null))
        assertError(map, "Illegal character", "SyntaxException", "\u0003")
    }
}