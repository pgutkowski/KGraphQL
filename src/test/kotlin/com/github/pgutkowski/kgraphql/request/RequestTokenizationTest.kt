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
        val (fragments, graph) = split(tokens)
        assertThat(fragments, equalTo(expectedFragments))
        assertThat(graph, equalTo(expectedGraph))
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

    @Test
    fun testSimpleSplit(){
        testSplit(
                input = "{hero {id ...heroName}} fragment heroName on Hero {name {real asHero}}",
                expectedFragments = listOf("fragment", "heroName", "on", "Hero", "{", "name", "{", "real", "asHero", "}", "}"),
                expectedGraph = listOf("{", "hero", "{", "id", "...heroName", "}", "}")
        )
    }

    @Test
    fun testSplitTwoFragments(){
        testSplit(
                input = "{hero {id ...heroName}} fragment heroName {name {real asHero}} fragment powers {name}",
                expectedFragments = listOf("fragment", "heroName", "{", "name", "{", "real", "asHero", "}", "}",
                        "fragment", "powers", "{", "name", "}"
                ),
                expectedGraph = listOf("{", "hero", "{", "id", "...heroName", "}", "}")
        )
    }

    @Test
    fun testSplitTwoSeparatedFragments(){
        testSplit(
                input = "fragment heroName {name {real asHero}} {hero {id ...heroName}} fragment powers {name}",
                expectedFragments = listOf("fragment", "heroName", "{", "name", "{", "real", "asHero", "}", "}",
                        "fragment", "powers", "{", "name", "}"
                ),
                expectedGraph = listOf("{", "hero", "{", "id", "...heroName", "}", "}")
        )
    }
}