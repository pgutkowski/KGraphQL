package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Specification("2.2 Query Document/2.3 Operations")
class QueryDocumentSpecificationTest {

    val schema = defaultSchema {
        query {
            name = "fizz"
            resolver{ -> "buzz"}
        }

        mutation {
            name = "createActor"
            resolver { name : String -> Actor(name, 11) }
        }
    }

    @Test
    fun `unnamed and named queries are equivalent`(){
        executeEqualQueries( schema,
                mapOf("data" to mapOf("fizz" to "buzz")),
                "{fizz}",
                "query {fizz}",
                "query BUZZ {fizz}"
        )
    }

    @Test
    fun `unnamed and named mutations are equivalent`(){
        executeEqualQueries( schema,
                mapOf("data" to mapOf("createActor" to mapOf("name" to "Kurt Russel"))),
                "{createActor(name : \"Kurt Russel\"){name}}",
                "mutation {createActor(name : \"Kurt Russel\"){name}}",
                "mutation KURT {createActor(name : \"Kurt Russel\"){name}}"
        )
    }

    @Test
    fun `anonymous operation must be the only defined operation`(){
        expect<SyntaxException>("anonymous operation must be the only defined operation"){
            deserialize(schema.execute("query {fizz} mutation BUZZ {createActor(name : \"Kurt Russel\"){name}}"))
        }
    }

    @Test
    fun `must provide operation name when multiple named operations`(){
        expect<SyntaxException>("Must provide an operation name from: [FIZZ, BUZZ]"){
            deserialize(schema.execute("query FIZZ {fizz} mutation BUZZ {createActor(name : \"Kurt Russel\"){name}}"))
        }
    }

    @Test
    fun `execute operation by name in variable`(){
        val map = deserialize(schema.execute (
                "query FIZZ {fizz} mutation BUZZ {createActor(name : \"Kurt Russel\"){name}}",
                "{\"operationName\":\"FIZZ\"}"
        ))
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/fizz"), equalTo("buzz"))
    }

    @Test
    @Disabled("Feature not supported yet")
    fun `handle subscription`(){
        Assertions.fail("Feature not supported yet")
    }
}