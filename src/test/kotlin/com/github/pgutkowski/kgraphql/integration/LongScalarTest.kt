package com.github.pgutkowski.kgraphql.integration

import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.specification.typesystem.ScalarsSpecificationTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class LongScalarTest {

    @Test
    fun testLongField(){
        val schema = defaultSchema {
            query("long"){
                resolver { -> Long.MAX_VALUE }
            }
        }

        val response = schema.execute("{long}")
        val long = deserialize(response).extract<Long>("data/long")
        assertThat(long, equalTo(Long.MAX_VALUE))
    }

    @Test
    fun testLongArgument(){
        val schema = defaultSchema {
            query("isLong"){
                resolver { long: Long -> if(long > Int.MAX_VALUE) "YES" else "NO" }
            }
        }

        val isLong = deserialize(schema.execute("{isLong(long: ${Int.MAX_VALUE.toLong() + 1})}")).extract<String>("data/isLong")
        assertThat(isLong, equalTo("YES"))
    }

    data class VeryLong(val long: Long)

    @Test
    fun `Schema may declare custom long scalar type`(){
        val schema = KGraphQL.schema {
            longScalar<VeryLong> {
                deserialize = ::VeryLong
                serialize = { (long) -> long }
            }

            query("number"){
                resolver { number : VeryLong -> number }
            }
        }

        val value = Int.MAX_VALUE.toLong() + 2
        val response = deserialize(schema.execute("{number(number: $value)}"))
        assertThat(response.extract<Long>("data/number"), equalTo(value))
    }

}