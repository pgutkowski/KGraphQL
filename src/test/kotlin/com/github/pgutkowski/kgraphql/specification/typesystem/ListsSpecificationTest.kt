package com.github.pgutkowski.kgraphql.specification.typesystem

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("3.1.7 Lists")
class ListsSpecificationTest{

    val objectMapper = jacksonObjectMapper()

    @Test
    fun `list arguments are valid`(){
        val schema = KGraphQL.schema {
            query("list"){
                resolver{ list: List<String> -> list }
            }
        }

        val variables = objectMapper.writeValueAsString(object {
            val list = listOf("GAGA", "DADA", "PADA")
        })

        val response = deserialize(schema.execute("query(\$list: [String]){list(list: \$list)}", variables))
        assertThat(extract<String>(response, "data/list[0]"), equalTo("GAGA"))
        assertThat(extract<String>(response, "data/list[1]"), equalTo("DADA"))
        assertThat(extract<String>(response, "data/list[2]"), equalTo("PADA"))
    }

    @Test
    fun `lists with nullable entries are valid`(){
        val schema = KGraphQL.schema {
            query("list"){
                resolver{ list: List<String?> -> list }
            }
        }

        val variables = objectMapper.writeValueAsString(object {
            val list = listOf("GAGA", null, "DADA", "PADA")
        })

        val response = deserialize(schema.execute("query(\$list: [String]){list(list: \$list)}", variables))
        assertThat(extract<String>(response, "data/list[1]"), nullValue())
    }

    @Test
    fun `lists with non-nullable entries should not accept list with null element`(){
        val schema = KGraphQL.schema {
            query("list"){
                resolver{ list: List<String> -> list }
            }
        }

        val variables = objectMapper.writeValueAsString(object {
            val list = listOf("GAGA", null, "DADA", "PADA")
        })

        val expectedMessage =
                "Invalid argument value [GAGA, null, DADA, PADA] from variable \$list, " +
                "expected list with non null arguments"
        expect<RequestException>(expectedMessage){
            schema.execute("query(\$list: [String]){list(list: \$list)}", variables)
        }
    }

    @Test
    fun `by default coerce single element input as collection`(){
        val schema = KGraphQL.schema {
            query("list"){
                resolver{ list: List<String> -> list }
            }
        }


        val variables = objectMapper.writeValueAsString(object {
            val list = "GAGA"
        })

        val response = deserialize(schema.execute("query(\$list: [String]){list(list: \$list)}", variables))
        assertThat(extract<String>(response, "data/list[0]"), equalTo("GAGA"))
    }

    @Test
    fun `null value is not coerced as single element collection`(){
        val schema = KGraphQL.schema {
            query("list"){
                resolver{ list: List<String>? -> list }
            }
        }


        val variables = objectMapper.writeValueAsString(object {
            val list = null
        })

        val response = deserialize(schema.execute("query(\$list: [String]){list(list: \$list)}", variables))
        assertThat(extract<String>(response, "data/list"), nullValue())
    }
}