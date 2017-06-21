package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("2.2 Query Document")
class QueryDocumentSpecificationTest {

    val schema = defaultSchema {
        query("fizz") {
            resolver{ -> "buzz"}
        }

        mutation("createActor") {
            resolver { name : String -> Actor(name, 11) }
        }
    }

    @Test
    fun `anonymous operation must be the only defined operation`(){
        expect<RequestException>("anonymous operation must be the only defined operation"){
            deserialize(schema.execute("query {fizz} mutation BUZZ {createActor(name : \"Kurt Russel\"){name}}"))
        }
    }

    @Test
    fun `must provide operation name when multiple named operations`(){
        expect<RequestException>("Must provide an operation name from: [FIZZ, BUZZ]"){
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
        assertThat(map.extract<String>("data/fizz"), equalTo("buzz"))
    }
}