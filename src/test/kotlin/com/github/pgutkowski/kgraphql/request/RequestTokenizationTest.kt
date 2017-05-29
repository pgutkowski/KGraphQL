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

    fun testSplit(input : String, expectedFragments: List<String>, expectedGraph : List<String>) {
        val tokens = tokenizeRequest(input)
//        val (fragments, graph) = createDocumentTokens(tokens)
//        assertThat(fragments, equalTo(expectedFragments))
//        assertThat(graph, equalTo(expectedGraph))
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

    @Test
    fun `split query and fragment`(){
        testSplit(
                input = "{hero {id ...heroName}} fragment heroName on Hero {name {real asHero}}",
                expectedFragments = listOf("fragment", "heroName", "on", "Hero", "{", "name", "{", "real", "asHero", "}", "}"),
                expectedGraph = listOf("{", "hero", "{", "id", "...heroName", "}", "}")
        )
    }

    @Test
    fun `split query and two fragments`(){
        testSplit(
                input = "{hero {id ...heroName}} fragment heroName {name {real asHero}} fragment powers {name}",
                expectedFragments = listOf("fragment", "heroName", "{", "name", "{", "real", "asHero", "}", "}",
                        "fragment", "powers", "{", "name", "}"
                ),
                expectedGraph = listOf("{", "hero", "{", "id", "...heroName", "}", "}")
        )
    }

    @Test
    fun `split two separated fragments`(){
        testSplit(
                input = "fragment heroName {name {real asHero}} {hero {id ...heroName}} fragment powers {name}",
                expectedFragments = listOf("fragment", "heroName", "{", "name", "{", "real", "asHero", "}", "}",
                        "fragment", "powers", "{", "name", "}"
                ),
                expectedGraph = listOf("{", "hero", "{", "id", "...heroName", "}", "}")
        )
    }
}