package com.github.pgutkowski.kgraphql.specification.typesystem

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("3.1.8 Non-null")
class NonNullSpecificationTest {

    @Test
    fun `if the result of non-null type is null, error should be raised`(){
        val schema = KGraphQL.schema {
            query("nonNull"){
                resolver { string : String? -> string!! }
            }
        }
        expect<ExecutionException>("Failed to invoke function nonNull"){
            schema.execute("{nonNull}")
        }
    }

    @Test
    fun `nullable input types are always optional`(){
        val schema = KGraphQL.schema {
            query("nullable"){
                resolver { input: String? -> input }
            }
        }

        val responseOmittedInput = deserialize(schema.execute("{nullable}"))
        assertThat(responseOmittedInput.extract<Any?>("data/nullable"), nullValue())

        val responseNullInput = deserialize(schema.execute("{nullable(input: null)}"))
        assertThat(responseNullInput.extract<Any?>("data/nullable"), nullValue())
    }

    @Test
    fun `non‐null types are always required`(){
        val schema = KGraphQL.schema {
            query("nonNull"){
                resolver { input: String -> input }
            }
        }

        expect<RequestException>("Missing value for non-nullable argument input"){
            schema.execute("{nonNull}")
        }
    }

    @Test
    fun `variable of a nullable type cannot be provided to a non-null argument`(){
        val schema = KGraphQL.schema {
            query("nonNull"){
                resolver { input: String -> input }
            }
        }

        schema.execute("query(\$arg: String!){nonNull(input: \$arg)}", "{\"arg\":\"SAD\"}")
    }

}