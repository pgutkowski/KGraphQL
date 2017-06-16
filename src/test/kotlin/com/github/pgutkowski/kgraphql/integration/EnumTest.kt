package com.github.pgutkowski.kgraphql.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test


class EnumTest : BaseSchemaTest() {

    @Test
    fun `query with enum field`(){
        val map = execute("{film{type}}")
        assertNoErrors(map)
        MatcherAssert.assertThat(extract<String>(map, "data/film/type"), CoreMatchers.equalTo("FULL_LENGTH"))
    }

    @Test
    fun `query with enum argument`(){
        val map = execute("{ films: filmsByType(type: FULL_LENGTH){title, type}}")
        assertNoErrors(map)
        MatcherAssert.assertThat(extract<String>(map, "data/films[0]/type"), CoreMatchers.equalTo("FULL_LENGTH"))
        MatcherAssert.assertThat(extract<String>(map, "data/films[1]/type"), CoreMatchers.equalTo("FULL_LENGTH"))
    }

//    @Test
//    fun `generator`(){
//        val objectNode = JsonNodeFactory.instance.objectNode()
//        objectNode.put("3434", 3434)
//        ObjectMapper().writeValueAsString(objectNode)
//    }
}