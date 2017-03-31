package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.SyntaxException
import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.annotation.method.ResolvingFunction
import com.github.pgutkowski.kql.deserialize
import com.github.pgutkowski.kql.extract
import com.github.pgutkowski.kql.resolve.QueryResolver
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test


class DefaultSchemaTest {

    val testFilm = TestClasses.Film(
            TestClasses.Id("Prestige", 2006),
            2006, "Prestige",
            TestClasses.Director(
                    "Christopher Nolan", 43,
                    listOf (
                            TestClasses.Actor("Tom Hardy", 232),
                            TestClasses.Actor("Christian Bale", 232)
                    )
            )
    )

    val testedSchema = DefaultSchemaBuilder()
            .addInput(TestClasses.InputClass::class)
            .addQueryField(TestClasses.Film::class, listOf(object: QueryResolver<TestClasses.Film> {
                @ResolvingFunction fun getQueryClass() : TestClasses.Film = testFilm
            }))
            .addScalar(TestClasses.Id::class, TestClasses.IdScalarSupport())
            .build() as DefaultSchema

    @Test
    fun testBasicResult(){
        val result = testedSchema.createResult("{film{title}}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(testFilm))
    }

    @Test
    fun testBasicResultWithImpliedFields(){
        val result = testedSchema.createResult("{film}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(testFilm))
    }

    @Test
    fun testNamedBasicResult(){
        val result = testedSchema.createResult("query Named {film{title}}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(testFilm))
    }

    @Test(expected = SyntaxException::class)
    fun testInvalidQueryMissingBracket(){
        testedSchema.createResult("query Named {film{title}")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBasicInvalidNamedQuery(){
        testedSchema.createResult("InvalidNamedQuery {film{title}}")
    }

    @Test
    fun testBasicJsonQuery(){
        val result = testedSchema.handleRequest("{film{title, director{name, age}}}")

        val map = deserialize(result)
        assertThat(map["data"], notNullValue())
        assertThat(map["errors"], nullValue())
        assertEquals(extract<String>(map, "data/film/title"), testFilm.title)
        assertEquals(extract<String>(map, "data/film/director/name"), testFilm.director.name)
        assertEquals(extract<Int>(map, "data/film/director/age"), testFilm.director.age)
    }

    @Test
    fun testCollections(){
        val result = testedSchema.handleRequest("{film{title, director{favActors}}}")

        val map = deserialize(result)
        assertThat(map["data"], notNullValue())
        assertThat(map["errors"], nullValue())
        assertEquals(extract<Map<*,*>>(map, "data/film/director/favActors[0]"), mapOf(
                "name" to testFilm.director.favActors[0].name,
                "age" to testFilm.director.favActors[0].age)
        )
    }

    /**
     *  Failing test: properties on collections are not properly handled yet
     */
    @Test
    fun testScalar(){
        val result = testedSchema.handleRequest("{film{id}}")

        val map = deserialize(result)
        assertThat(map["data"], notNullValue())
        assertThat(map["errors"], nullValue())
        assertEquals(extract<String>(map, "data/film/id"), "${testFilm.id.literal}:${testFilm.id.numeric}")
    }

    @Test
    @Ignore("To be implemented: properties on collections are not properly handled yet")
    fun testCollectionEntriesProperties(){
        val result = testedSchema.handleRequest("{film{title, director{favActors{name}}}}")

        val map = deserialize(result)
        assertThat(map["data"], notNullValue())
        assertThat(map["errors"], nullValue())
        assertEquals(extract<Map<*,*>>(map, "data/film/director/favActors[0]"), mapOf(
                "name" to testFilm.director.favActors[0].name,
                "age" to null)
        )
    }

    @Test
    fun testInvalidPropertyName(){
        val result = testedSchema.handleRequest("{film{title, director{name,[favActors]}}}")
        val map = deserialize(result)
        assertThat(map["data"], nullValue())
        assertThat(map["errors"], notNullValue())
        assertThat(extract<String>(map, "errors/message"), notNullValue())
    }
}