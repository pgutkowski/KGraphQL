package com.github.pgutkowski.kgraphql.request

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

/**
 * Not lots of tests, as tokenization is mostly covered by Query and Mutation tests
 */
class RequestTokenizationTest {

    fun testTokenization(input : String, expected : List<String>) {
        val tokens = tokenizeRequest(input)
        assertThat(tokens, equalTo(expected))
    }

    @Test
    fun `tokenize mutation with args`(){
        testTokenization(
                input = "{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\")}",
                expected = listOf("{", "createHero", "(", "name", ":", "\"Batman\"", "appearsIn", ":", "\"The Dark Knight\"", ")", "}")
        )
    }

    @Test
    fun `tokenize simple query`(){
        testTokenization(
                input = "{batman: hero(name: \"Batman\"){ skills : powers }}",
                expected = listOf("{", "batman", ":", "hero", "(", "name", ":", "\"Batman\"", ")", "{", "skills", ":", "powers", "}", "}")
        )
    }

    @Test
    fun `tokenize query with nested selection set`(){
        testTokenization(
                input = "{hero{name appearsIn{title{abbr full} year}}\nvillain{name deeds}}",
                expected = listOf(
                        "{", "hero", "{", "name", "appearsIn", "{", "title", "{", "abbr", "full", "}", "year", "}", "}",
                        "villain", "{", "name", "deeds", "}", "}"
                )
        )
    }
}