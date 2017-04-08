package com.github.pgutkowski.kql.request

import com.github.pgutkowski.kql.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class GraphParserTest {

    val graphParser = GraphParser()

    @Test
    fun nestedQueryParsing() {
        val map = graphParser.parse("{hero{name, appearsIn}\nvillain{name, [deeds]}}")
        val expected = Graph(
                branch( "hero",
                        leaf("name"),
                        leaf("appearsIn")
                ),
                branch( "villain",
                        leaf("name"),
                        leaf("[deeds]")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun doubleNestedQueryParsing() {
        val map = graphParser.parse("{hero{name, appearsIn{title, year}}\nvillain{name, [deeds]}}")

        val expected = Graph(
                branch( "hero",
                        leaf("name"),
                        branch("appearsIn",
                                leaf("title"),
                                leaf("year")
                        )
                ),
                branch( "villain",
                        leaf("name"),
                        leaf("[deeds]")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun tripleNestedQueryParsing() {
        val map = graphParser.parse("{hero{name, appearsIn{title{abbr, full}, year}}\nvillain{name, [deeds]}}")

        val expected = Graph(
                branch( "hero",
                        leaf("name"),
                        branch("appearsIn",
                                branch("title",
                                        leaf("abbr"),
                                        leaf("full")),
                                leaf("year")
                        )
                ),
                branch( "villain",
                        leaf("name"),
                        leaf("[deeds]")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun mutationArgumentsParsing(){
        val map = graphParser.parse("{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\")}")
        val expected = Graph (
                argLeaf("createHero", args("name" to "\"Batman\"", "appearsIn" to "\"The Dark Knight\""))
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun fieldArgumentsParsing(){
        val map = graphParser.parse("{hero{name, height(unit: FOOT)}}")
        val expected = Graph(
                branch("hero",
                        leaf("name"),
                        argLeaf("height", args("unit" to "FOOT"))
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun mutationFieldsParsing(){
        val map = graphParser.parse("{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\"){id, name, timestamp}}")
        val expected = Graph (
                argBranch("createHero",
                        args("name" to "\"Batman\"", "appearsIn" to "\"The Dark Knight\""),
                        *leafs("id", "name", "timestamp")
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun mutationNestedFieldsParsing(){
        val map = graphParser.parse("{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\"){id, name {real, asHero}}}")
        val expected = Graph (
                argBranch("createHero",
                        args("name" to "\"Batman\"", "appearsIn" to "\"The Dark Knight\""),
                        leaf("id"),
                        branch("name", *leafs("real", "asHero"))
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun testIndexOfClosingBracket(){
        fun executeTest(input : String, expected: Int? = null){
            val result = graphParser.indexOfClosingBracket(input)
            assertThat(result, equalTo(expected ?: input.length -1))
        }
        executeTest("{hero{name, appearsIn{title, year}}}")
        executeTest("{hero{name{first, second}, appearsIn{title, year}}}")
        executeTest("{hero}")

        executeTest("{hero}, movie{year, title}}", 5)
        executeTest("{hero{powers{primary}}}\nvillain{deeds}}", 22)
    }
}