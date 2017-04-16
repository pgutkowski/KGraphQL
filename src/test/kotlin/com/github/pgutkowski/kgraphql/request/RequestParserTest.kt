package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.Graph.Companion.leaf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.Test


class RequestParserTest {

    val requestParser = RequestParser({
        if(it.contains("literal", true) || it.contains("name", true)){
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
        assertThat(requestParser.dropRequestHeader("query CoolQuery { literal, name }"), equalTo("{ literal, name }"))
    }

    @Test
    fun testInferType(){
        assertThat(requestParser.parse("query CoolQuery { literal, name }").action, equalTo(Request.Action.QUERY))
        assertThat(requestParser.parse("{ literal, name }").action, equalTo(Request.Action.QUERY))

        assertThat(requestParser.parse("mutation HotMutation { age, date }").action, equalTo(Request.Action.MUTATION))
        assertThat(requestParser.parse("{ age, date }").action, equalTo(Request.Action.MUTATION))
    }

    @Test
    fun testParsing(){
        val result = requestParser.parse("query CoolQuery { literal, name }")
        assertThat(result.action, equalTo(Request.Action.QUERY))
        assertThat(result.name, equalTo("CoolQuery"))
        assertThat(result.graph, equalTo(Graph(leaf("literal"), leaf("name"))))
    }
}