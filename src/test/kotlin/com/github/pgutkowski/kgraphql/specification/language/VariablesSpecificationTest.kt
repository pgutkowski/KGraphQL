package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.integration.BaseSchemaTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("2.10 Variables")
class VariablesSpecificationTest : BaseSchemaTest() {
    @Test
    fun `query with variables`(){
        val map = execute(
                query = "mutation(\$name: String!, \$age : Int!) {createActor(name: \$name, age: \$age){name, age}}",
                variables = "{\"name\":\"Boguś Linda\", \"age\": 22}"
        )
        assertNoErrors(map)
        assertThat(
                map.extract<Map<String, Any>>("data/createActor"),
                equalTo(mapOf("name" to "Boguś Linda", "age" to 22))
        )
    }

    @Test
    fun `query with boolean variable`(){
        val map = execute(query = "query(\$big: Boolean!) {number(big: \$big)}", variables = "{\"big\": true}")
        assertNoErrors(map)
        assertThat(map.extract<Int>("data/number"), equalTo(10000))
    }

    @Test
    fun `query with boolean variable default value`(){
        val map = execute(query = "query(\$big: Boolean = true) {number(big: \$big)}")
        assertNoErrors(map)
        assertThat(map.extract<Int>("data/number"), equalTo(10000))
    }

    @Test
    fun `query with variables and string default value`(){
        val map = execute(
                query = "mutation(\$name: String = \"Boguś Linda\", \$age : Int!) {createActor(name: \$name, age: \$age){name, age}}",
                variables = "{\"age\": 22}"
        )
        assertNoErrors(map)
        assertThat(
                map.extract<Map<String, Any>>("data/createActor"),
                equalTo(mapOf("name" to "Boguś Linda", "age" to 22))
        )
    }

    @Test
    fun `query with variables and default value pointing to another variable`(){
        val map = execute(
                query = "mutation(\$name: String = \"Boguś Linda\", \$age : Int = \$defaultAge, \$defaultAge : Int!) " +
                        "{createActor(name: \$name, age: \$age){name, age}}",
                variables = "{\"defaultAge\": 22}"
        )
        assertNoErrors(map)
        assertThat(
                map.extract<Map<String, Any>>("data/createActor"),
                equalTo(mapOf("name" to "Boguś Linda", "age" to 22))
        )
    }

    @Test
    fun `fragment with variable`(){
        val map = execute(
                query = "mutation(\$name: String = \"Boguś Linda\", \$age : Int!, \$big: Boolean!) {createActor(name: \$name, age: \$age){...Linda}}" +
                        "fragment Linda on Actor {picture(big: \$big)}",
                variables = "{\"age\": 22, \"big\": true}"
        )
        assertNoErrors(map)
        assertThat(
                map.extract<String>("data/createActor/picture"),
                equalTo("http://picture.server/pic/Boguś_Linda?big=true")
        )
    }

    @Test
    fun `fragment with missing variable`(){
        expect<IllegalArgumentException>("Variable '\$big' was not declared for this operation"){
            execute(
                    query = "mutation(\$name: String = \"Boguś Linda\", \$age : Int!) {createActor(name: \$name, age: \$age){...Linda}}" +
                            "fragment Linda on Actor {picture(big: \$big)}",
                    variables = "{\"age\": 22}"
            )
        }
    }
}