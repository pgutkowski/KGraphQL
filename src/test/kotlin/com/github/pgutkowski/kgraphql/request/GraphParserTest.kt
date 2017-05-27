package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.graph.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


class GraphParserTest {

    val graphParser = GraphParser()

    @Test
    fun nestedQueryParsing() {
        val map = graphParser.parse("{hero{name appearsIn}\nvillain{name deeds}}")
        val expected = Graph(
                branch( "hero",
                        leaf("name"),
                        leaf("appearsIn")
                ),
                branch( "villain",
                        leaf("name"),
                        leaf("deeds")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun doubleNestedQueryParsing() {
        val map = graphParser.parse("{hero{name appearsIn{title, year}}\nvillain{name deeds}}")

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
                        leaf("deeds")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun tripleNestedQueryParsing() {
        val map = graphParser.parse("{hero{name appearsIn{title{abbr full} year}}\nvillain{name deeds}}")

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
                        leaf("deeds")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun queryWithArguments() {
        val map = graphParser.parse("{hero(name: \"Batman\"){ power }}")

        val expected = Graph(
                argBranch("hero",
                        args("name" to "\"Batman\""),
                        leaf("power")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun queryWithAlias() {
        val map = graphParser.parse("{batman: hero(name: \"Batman\"){ power }}")

        val expected = Graph(
                argBranch("hero", "batman",
                        args("name" to "\"Batman\""),
                        leaf("power")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun queryWithFieldAlias() {
        val map = graphParser.parse("{batman: hero(name: \"Batman\"){ skills : powers }}")

        val expected = Graph(
                argBranch("hero", "batman",
                        args("name" to "\"Batman\""),
                        leaf("powers", "skills")
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
        val map = graphParser.parse("{hero{name height(unit: FOOT)}}")
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
        val map = graphParser.parse("{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\"){id name timestamp}}")
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
        val map = graphParser.parse("{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\"){id name {real asHero}}}")
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
    fun testSimpleFragment(){
        val map = graphParser.parse("{hero {id ...heroName}} fragment heroName {name {real asHero}}")
        val expected = Graph (
                branch("hero",
                        leaf("id"),
                        extFragment( "...heroName",
                                branch("name", *leafs("real", "asHero"))
                        )
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun testTypedFragment(){
        val map = graphParser.parse("{hero {id ...heroName}} fragment heroName on Hero {name {real asHero}}")
        val expected = Graph (
                branch("hero",
                        leaf("id"),
                        extFragment( "...heroName", "Hero",
                                branch("name", *leafs("real", "asHero"))
                        )
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun testInlineFragment(){
        val map = graphParser.parse("{hero {id ... on Hero {height, power}}}")
        val expected = Graph (
                branch("hero",
                        leaf("id"),
                        inlineFragment("Hero",
                                leaf("height"),
                                leaf("power")
                        )
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun testTwoInlineFragments(){
        val map = graphParser.parse("{hero {id ... on Hero {height, power}, ... on Villain {height, deeds}}}")
        val expected = Graph (
                branch("hero",
                        leaf("id"),
                        inlineFragment("Hero",
                                leaf("height"),
                                leaf("power")
                        ),
                        inlineFragment("Villain",
                                leaf("height"),
                                leaf("deeds")
                        )
                )
        )
        assertThat(map, equalTo(expected))
    }
}