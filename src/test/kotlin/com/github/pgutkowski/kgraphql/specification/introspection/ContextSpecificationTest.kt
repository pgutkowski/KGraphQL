package com.github.pgutkowski.kgraphql.specification.introspection

import com.github.pgutkowski.kgraphql.Context
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

class ContextSpecificationTest {
  @Test
  fun `query resolver should not return context param`() {
    val schema = defaultSchema {
      query("sample") {
        resolver { ctx: Context, limit: Int -> "SAMPLE" }
      }
    }

    val response = deserialize(schema.execute("{__schema{queryType{fields{args{name}}}}}"))
    println("response: $response")
    MatcherAssert.assertThat(response.extract("data/__schema/queryType/fields[0]/args[0]/name"), CoreMatchers.equalTo("limit"))
  }

  @Test
  fun `mutation resolver should not return context param`() {
    val schema = defaultSchema {
      mutation("sample") {
        resolver { ctx: Context, input: String -> "SAMPLE" }
      }
    }

    val response = deserialize(schema.execute("{__schema{mutationType{fields{args{name}}}}}"))
    println("response: $response")
    MatcherAssert.assertThat(response.extract("data/__schema/mutationType/fields[0]/args[0]/name"), CoreMatchers.equalTo("input"))
  }
}