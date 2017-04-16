package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class QueryTest : BaseSchemaTest() {
    @Test
    fun testBasicJsonQuery(){
        val map = execute("{film{title, director{name, age}}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/title"), equalTo(testFilm.title))
        assertThat(extract<String>(map, "data/film/director/name"), equalTo(testFilm.director.name))
        assertThat(extract<Int>(map, "data/film/director/age"), equalTo(testFilm.director.age))
    }

    @Test
    fun testCollections(){
        val map = execute("{film{title, director{favActors}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, String>>(map, "data/film/director/favActors[0]"), equalTo(mapOf(
                "name" to testFilm.director.favActors[0].name,
                "age" to testFilm.director.favActors[0].age)
        ))
    }

    @Test
    fun testScalar(){
        val map = execute("{film{id}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/id"), equalTo("${testFilm.id.literal}:${testFilm.id.numeric}"))
    }

    @Test
    fun testScalarImplicit(){
        val map = execute("{film}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/id"), equalTo("${testFilm.id.literal}:${testFilm.id.numeric}"))
    }

    @Test
    fun testCollectionEntriesProperties(){
        val map = execute("{film{title, director{favActors{name}}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, String>>(map, "data/film/director/favActors[0]"), equalTo(mapOf("name" to testFilm.director.favActors[0].name)))
    }

    @Test
    fun testCollectionEntriesProperties2(){
        val map = execute("{film{title, director{favActors{age}}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Int>>(map, "data/film/director/favActors[0]"), equalTo(mapOf("age" to testFilm.director.favActors[0].age)))
    }

    @Test
    fun testInvalidPropertyName(){
        val map = execute("{film{title, director{name,[favActors]}}}")
        assertError(map, "Cannot find property", "[favActors]")
    }

    @Test
    fun testQueryWithArgument(){
        val map = execute("{filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/title"), equalTo("Prestige"))
    }

    @Test
    fun testQueryWithArgument2(){
        val map = execute("{filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/title"), equalTo("Se7en"))
    }

    @Test
    fun testQueryWithAlias(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/bestFilm/title"), equalTo("Prestige"))
    }

    @Test
    fun testQueryWithFieldAlias(){
        val map =execute("{filmByRank(rank: 2){fullTitle: title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/fullTitle"), equalTo("Se7en"))
    }

    @Test
    fun testQueryWithAliases(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}, secondBestFilm: filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/bestFilm/title"), equalTo("Prestige"))
        assertThat(extract<String>(map, "data/secondBestFilm/title"), equalTo("Se7en"))
    }

    @Test
    fun testInvalidQueryWithDuplicatedAliases(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}, bestFilm: filmByRank(rank: 2){title}}")
        assertError(map, "SyntaxException: Duplicated property name/alias: bestFilm")
    }
}