package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test


class VariablesTest : BaseSchemaTest() {

    @Test
    fun testQueryWithVariables(){
        val map = execute("mutation {createActor(name: \$name, age: \$age){name, age}}", "{\"name\":\"Boguś Linda\", \"age\": 22}")
        assertNoErrors(map)
        MatcherAssert.assertThat(extract<Map<String, Any>>(map, "data/createActor"), CoreMatchers.equalTo(mapOf("name" to "Boguś Linda", "age" to 22)))
    }
}