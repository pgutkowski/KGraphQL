package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.graph.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


class DocumentParserTest {

    val graphParser = DocumentParser()

    @Test
    fun `nested query parsing`() {
        val map = graphParser.parseSelectionTree("{hero{name appearsIn}\nvillain{name deeds}}")
        val expected = SelectionTree(
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
    fun `double nested query parsing`() {
        val map = graphParser.parseSelectionTree("{hero{name appearsIn{title, year}}\nvillain{name deeds}}")

        val expected = SelectionTree(
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
    fun `triple nested query parsing`() {
        val map = graphParser.parseSelectionTree("{hero{name appearsIn{title{abbr full} year}}\nvillain{name deeds}}")

        val expected = SelectionTree(
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
    fun `query with arguments parsing`() {
        val map = graphParser.parseSelectionTree("{hero(name: \"Batman\"){ power }}")

        val expected = SelectionTree(
                argBranch("hero",
                        args("name" to "\"Batman\""),
                        leaf("power")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun `query with alias parsing`() {
        val map = graphParser.parseSelectionTree("{batman: hero(name: \"Batman\"){ power }}")

        val expected = SelectionTree(
                argBranch("hero", "batman",
                        args("name" to "\"Batman\""),
                        leaf("power")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun `query with field alias parsing`() {
        val map = graphParser.parseSelectionTree("{batman: hero(name: \"Batman\"){ skills : powers }}")

        val expected = SelectionTree(
                argBranch("hero", "batman",
                        args("name" to "\"Batman\""),
                        leaf("powers", "skills")
                )
        )

        assertThat(map, equalTo(expected))
    }

    @Test
    fun `mutation with field alias parsing`(){
        val map = graphParser.parseSelectionTree(("{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\")}"))
        val expected = SelectionTree(
                argLeaf("createHero", args("name" to "\"Batman\"", "appearsIn" to "\"The Dark Knight\""))
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun `field arguments parsing`(){
        val map = graphParser.parseSelectionTree(("{hero{name height(unit: FOOT)}}"))
        val expected = SelectionTree(
                branch("hero",
                        leaf("name"),
                        argLeaf("height", args("unit" to "FOOT"))
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun `mutation selection set parsing`(){
        val map = graphParser.parseSelectionTree(("{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\"){id name timestamp}}"))
        val expected = SelectionTree(
                argBranch("createHero",
                        args("name" to "\"Batman\"", "appearsIn" to "\"The Dark Knight\""),
                        *leafs("id", "name", "timestamp")
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun `mutation nested selection set parsing`(){
        val map = graphParser.parseSelectionTree(("{createHero(name: \"Batman\", appearsIn: \"The Dark Knight\"){id name {real asHero}}}"))
        val expected = SelectionTree(
                argBranch("createHero",
                        args("name" to "\"Batman\"", "appearsIn" to "\"The Dark Knight\""),
                        leaf("id"),
                        branch("name", *leafs("real", "asHero"))
                )
        )
        assertThat(map, equalTo(expected))
    }

    @Test
    fun `fragment parsing`(){
        val map = graphParser.parseDocument("{hero {id ...heroName}} fragment heroName on Hero {name {real asHero}}")
        val expected = SelectionTree(
                branch("hero",
                        leaf("id"),
                        extFragment( "...heroName", "Hero", branch("name", *leafs("real", "asHero")))
                )
        )
        assertThat(map.first().selectionTree, equalTo(expected))
    }

    @Test
    fun `fragment with type condition parsing`(){
        val map = graphParser.parseDocument("{hero {id ...heroName}} fragment heroName on Hero {name {real asHero}}")
        val expected = SelectionTree(
                branch("hero",
                        leaf("id"),
                        extFragment( "...heroName", "Hero",
                                branch("name", *leafs("real", "asHero"))
                        )
                )
        )
        assertThat(map.first().selectionTree, equalTo(expected))
    }

    @Test
    fun `inline fragment parsing`(){
        val map = graphParser.parseDocument("{hero {id ... on Hero {height, power}}}")
        val expected = SelectionTree(
                branch("hero",
                        leaf("id"),
                        inlineFragment("Hero",
                                leaf("height"),
                                leaf("power")
                        )
                )
        )
        assertThat(map.first().selectionTree, equalTo(expected))
    }

    @Test
    fun `two inline fragments parsing`(){
        val map = graphParser.parseDocument("{hero {id ... on Hero {height, power}, ... on Villain {height, deeds}}}")
        val expected = SelectionTree(
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
        assertThat(map.first().selectionTree, equalTo(expected))
    }
}