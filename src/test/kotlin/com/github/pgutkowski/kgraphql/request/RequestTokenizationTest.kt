package com.github.pgutkowski.kgraphql.request

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

/**
 * Not lots of tests, as tokenization is mostly covered by Query and Mutation tests
 */
class RequestTokenizationTest {

    fun testTokenization(input : String, expected : List<String>) {
        val tokens = tokenizeRequest(input)
        assertThat(tokens, equalTo(expected))
    }

    @Test
    fun tokenizeMutationWithArgs(){
        testTokenization(
                input = "{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\")}",
                expected = listOf("{", "createHero", "(", "name", ":", "\"Batman\"", "appearsIn", ":", "\"The Dark Knight\"", ")", "}")
        )
    }

    @Test
    fun tokenizeSimpleQuery(){
        testTokenization(
                input = "{batman: hero(name: \"Batman\"){ skills : powers }}",
                expected = listOf("{", "batman", ":", "hero", "(", "name", ":", "\"Batman\"", ")", "{", "skills", ":", "powers", "}", "}")
        )
    }

    @Test
    fun tokenizeNestedQuery(){
        testTokenization(
                input = "{hero{name appearsIn{title{abbr full} year}}\nvillain{name deeds}}",
                expected = listOf(
                        "{", "hero", "{", "name", "appearsIn", "{", "title", "{", "abbr", "full", "}", "year", "}", "}",
                        "villain", "{", "name", "deeds", "}", "}"
                )
        )
    }

}