package com.github.pgutkowski.kql.schema.impl

import com.fasterxml.jackson.databind.JsonMappingException
import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.annotation.method.ResolvingFunction
import com.github.pgutkowski.kql.Graph
import com.github.pgutkowski.kql.resolve.QueryResolver
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class DefaultSchemaTest {

    val testReturnValue = TestClasses.Film(2006, "Prestige", TestClasses.Director("Christopher Nolan", 43, listOf("Tom Hardy")))

    val testedSchema = DefaultSchemaBuilder()
            .addInput(TestClasses.InputClass::class)
            .addQueryField(TestClasses.Film::class, listOf(object: QueryResolver<TestClasses.Film> {
                @ResolvingFunction fun getQueryClass() : TestClasses.Film = testReturnValue
            }))
            .build()

    @Test
    fun testBasicQuery(){
        val result = testedSchema.handleRequest("{film{title}}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(testReturnValue))
    }

    @Test
    fun testBasicQueryWithImpliedFields(){
        val result = testedSchema.handleRequest("{film}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(testReturnValue))
    }

    @Test
    fun testNamedBasicQuery(){
        val result = testedSchema.handleRequest("query Named {film{title}}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(testReturnValue))
    }

    @Test
    fun testInvalidQueryMissingBracket(){
        val result = testedSchema.handleRequest("query Named {film{title}")
        assertThat(result.errors, notNullValue())
        assertThat(result.data, nullValue())
    }

    @Test
    fun testBasicInvalidNamedQuery(){
        val result = testedSchema.handleRequest("InvalidNamedQuery {film{title}}")
        assertThat(result.errors, notNullValue())
        assertThat(result.data, nullValue())
    }

    @Test
    fun testBasicJsonQuery(){
        val result = testedSchema.handleRequestAsJson("{film{title, director{name}}}")
        //TODO: find better way for JSON result verification
        assertThat(result, equalTo("{\"data\":{\"film\":{\"director\":{\"name\":\"Christopher Nolan\"},\"title\":\"Prestige\"}}}"))
    }


    /**
     * Failing tests to be fixed
     */
    @Test(expected = JsonMappingException::class)
    fun testBasicJsonQueryInvalidProperty(){
        val result = testedSchema.handleRequestAsJson("{film{title, director{favActors}}}")
    }

    @Test
    fun testBasicJsonQueryNotHandlingCollectionsYet(){
        val result = testedSchema.handleRequestAsJson("{film{title, director{[favActors]}}}")
    }
}