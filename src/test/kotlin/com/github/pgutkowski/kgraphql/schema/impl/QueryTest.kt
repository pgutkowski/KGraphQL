package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.extract
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class QueryTest : BaseSchemaTest() {
    @Test
    fun testBasicJsonQuery(){
        val result = testedSchema.handleRequest("{film{title, director{name, age}}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/title"), equalTo(testFilm.title))
        assertThat(extract<String>(map, "data/film/director/name"), equalTo(testFilm.director.name))
        assertThat(extract<Int>(map, "data/film/director/age"), equalTo(testFilm.director.age))
    }

    @Test
    fun testCollections(){
        val result = testedSchema.handleRequest("{film{title, director{favActors}}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertThat(extract<Map<String, String>>(map, "data/film/director/favActors[0]"), equalTo(mapOf(
                "name" to testFilm.director.favActors[0].name,
                "age" to testFilm.director.favActors[0].age)
        ))
    }

    @Test
    fun testScalar(){
        val result = testedSchema.handleRequest("{film{id}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/id"), equalTo("${testFilm.id.literal}:${testFilm.id.numeric}"))
    }

    @Test
    fun testCollectionEntriesProperties(){
        val result = testedSchema.handleRequest("{film{title, director{favActors{name}}}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertThat(extract<Map<String, String>>(map, "data/film/director/favActors[0]"), equalTo(mapOf("name" to testFilm.director.favActors[0].name)))
    }

    @Test
    fun testCollectionEntriesProperties2(){
        val result = testedSchema.handleRequest("{film{title, director{favActors{age}}}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertThat(extract<Map<String, Int>>(map, "data/film/director/favActors[0]"), equalTo(mapOf("age" to testFilm.director.favActors[0].age)))
    }

    @Test
    fun testInvalidPropertyName(){
        val result = testedSchema.handleRequest("{film{title, director{name,[favActors]}}}")
        val map = deserialize(result)
        assertError(map, "Cannot find property", "[favActors]")
    }
}