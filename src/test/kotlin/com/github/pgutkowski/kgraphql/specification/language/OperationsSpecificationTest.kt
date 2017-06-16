package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.executeEqualQueries
import junit.framework.Assert.fail
import org.junit.Ignore
import org.junit.Test

@Specification("2.3 Operations")
class OperationsSpecificationTest {

    val schema = defaultSchema {
        query("fizz") {
            resolver{ -> "buzz"}
        }

        mutation("createActor") {
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
    @Ignore("Feature not supported yet")
    fun `handle subscription`(){
        fail("Feature not supported yet")
    }
}