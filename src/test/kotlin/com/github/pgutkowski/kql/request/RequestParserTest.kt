package com.github.pgutkowski.kql.request

import com.github.pgutkowski.kql.Graph
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Test


class RequestParserTest {

    val requestParser = RequestParser({
        if(it.contains("id", true) || it.contains("name", true)){
            Request.Action.QUERY
        } else{
            Request.Action.MUTATION
        }
    })

    @Test
    fun getRequestHeaderTokens(){
        assertThat(requestParser.getRequestHeaderTokens("{}"), hasSize(0))
        assertThat(requestParser.getRequestHeaderTokens("query {}"), hasSize(1))
        assertThat(requestParser.getRequestHeaderTokens("query CoolQuery {}"), hasSize(2))
    }

    @Test
    fun dropRequestHeader(){
        assertThat(requestParser.dropRequestHeader("{}"), equalTo("{}"))
        assertThat(requestParser.dropRequestHeader("query {}"), equalTo("{}"))
        assertThat(requestParser.dropRequestHeader("query CoolQuery { id, name }"), equalTo("{ id, name }"))
    }

    @Test
    fun testInferType(){
        assertThat(requestParser.parse("query CoolQuery { id, name }").action, equalTo(Request.Action.QUERY))
        assertThat(requestParser.parse("{ id, name }").action, equalTo(Request.Action.QUERY))

        assertThat(requestParser.parse("mutation HotMutation { age, date }").action, equalTo(Request.Action.MUTATION))
        assertThat(requestParser.parse("{ age, date }").action, equalTo(Request.Action.MUTATION))
    }

    @Test
    fun testParsing(){
        val result = requestParser.parse("query CoolQuery { id, name }")
        assertThat(result.action, equalTo(Request.Action.QUERY))
        assertThat(result.name, equalTo("CoolQuery"))
        assertThat(result.graph, equalTo(Graph("id" to null, "name" to null)))
    }
}