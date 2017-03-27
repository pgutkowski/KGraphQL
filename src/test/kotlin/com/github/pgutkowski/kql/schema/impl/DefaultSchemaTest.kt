package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.annotation.method.ResolvingFunction
import com.github.pgutkowski.kql.request.Graph
import com.github.pgutkowski.kql.resolve.QueryResolver
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class DefaultSchemaTest {

    val testedSchema = DefaultSchemaBuilder()
            .addInput(TestClasses.InputClass::class)
            .addQueryField(TestClasses.Film::class, listOf(object: QueryResolver<TestClasses.Film> {
                @ResolvingFunction fun getQueryClass() : TestClasses.Film = TestClasses.Film(2006, "Prestige")
            }))
            .build()

    @Test
    fun testBasicQuery(){
        val result = testedSchema.handleRequest("{film{title}}")
        assertThat(result.errors, nullValue())
        assertThat(result.data, equalTo(Graph("film" to Graph("title" to "Prestige"))))
    }

    @Test
    fun testBasicQueryWithImpliedFields(){
        val result = testedSchema.handleRequest("{film}")
        assertThat(result.errors, nullValue())
        assertThat(result.data, equalTo(Graph("film" to Graph("title" to "Prestige", "year" to 2006))))
    }

    @Test
    fun testNamedBasicQuery(){
        val result = testedSchema.handleRequest("query Named {film{title}}")
        assertThat(result.errors, nullValue())
        assertThat(result.data, equalTo(Graph("film" to Graph("title" to "Prestige"))))
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
}