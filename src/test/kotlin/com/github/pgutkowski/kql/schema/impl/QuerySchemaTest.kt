package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.deserialize
import com.github.pgutkowski.kql.extract
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test


class QuerySchemaTest : BaseSchemaTest() {
    @Test
    fun testBasicJsonQuery(){
        val result = testedSchema.handleRequest("{film{title, director{name, age}}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertEquals(extract<String>(map, "data/film/title"), testFilm.title)
        assertEquals(extract<String>(map, "data/film/director/name"), testFilm.director.name)
        assertEquals(extract<Int>(map, "data/film/director/age"), testFilm.director.age)
    }

    @Test
    fun testCollections(){
        val result = testedSchema.handleRequest("{film{title, director{favActors}}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertEquals(extract<Map<*, *>>(map, "data/film/director/favActors[0]"), mapOf(
                "name" to testFilm.director.favActors[0].name,
                "age" to testFilm.director.favActors[0].age)
        )
    }

    @Test
    fun testScalar(){
        val result = testedSchema.handleRequest("{film{id}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertEquals(extract<String>(map, "data/film/id"), "${testFilm.id.literal}:${testFilm.id.numeric}")
    }

    @Test
    fun testCollectionEntriesProperties(){
        val result = testedSchema.handleRequest("{film{title, director{favActors{name}}}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertEquals(extract<Map<*, *>>(map, "data/film/director/favActors[0]"), mapOf("name" to testFilm.director.favActors[0].name))
    }

    @Test
    fun testCollectionEntriesProperties2(){
        val result = testedSchema.handleRequest("{film{title, director{favActors{age}}}}")

        val map = deserialize(result)
        assertNoErrors(map)
        assertEquals(extract<Map<*, *>>(map, "data/film/director/favActors[0]"), mapOf("age" to testFilm.director.favActors[0].age))
    }

    @Test
    fun testInvalidPropertyName(){
        val result = testedSchema.handleRequest("{film{title, director{name,[favActors]}}}")
        val map = deserialize(result)
        MatcherAssert.assertThat(extract<String>(map, "errors/message"), CoreMatchers.notNullValue())
    }
}