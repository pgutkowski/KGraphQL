package com.github.pgutkowski.kgraphql.specification.typesystem

import com.github.pgutkowski.kgraphql.*
import com.github.pgutkowski.kgraphql.schema.dsl.enum
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


@Specification("3.1.5 Enums")
class EnumsSpecificationTest {

    enum class Coolness {
        NOT_COOL, COOL, TOTALLY_COOL
    }

    val schema = KGraphQL.schema {
        enum<Coolness>()
        query("cool"){
            resolver{ cool: Coolness -> cool.toString() }
        }
    }

    @Test
    fun `string literals must not be accepted as an enum input`(){
        expect<RequestException>("Invalid string literal value '\"COOL\"' for enum Coolness"){
            schema.execute("{cool(cool : \"COOL\")}")
        }
    }

    @Test
    fun `string constants are accepted as an enum input`(){
        val response = deserialize(schema.execute("{cool(cool : COOL)}"))
        assertThat(extract<String>(response, "data/cool"), equalTo("COOL"))
    }

}