package com.github.pgutkowski.kgraphql.integration

import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test


class VariablesTest : BaseSchemaTest() {

    @Test
    fun `query with variables`(){
        val map = execute(
                query = "mutation {createActor(name: \$name, age: \$age){name, age}}",
                variables = "{\"name\":\"Boguś Linda\", \"age\": 22}"
        )
        assertNoErrors(map)
        MatcherAssert.assertThat(
                extract<Map<String, Any>>(map, "data/createActor"),
                equalTo(mapOf("name" to "Boguś Linda", "age" to 22))
        )
    }

    @Test
    fun `query with boolean variable`(){
        val map = execute(query = "query {number(big: \$big)}", variables = "{\"big\": true}")
        assertNoErrors(map)
        MatcherAssert.assertThat(extract<Int>(map, "data/number"), equalTo(10000))
    }
}