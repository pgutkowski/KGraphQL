package com.github.pgutkowski.kgraphql.integration

import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.deserialize
import kotlinx.coroutines.delay
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

class ParallelExecutionTest {

    val syncResolversSchema = KGraphQL.schema {
        repeat(1000) {
            query("automated-${it}") {
                resolver { ->
                    Thread.sleep(3)
                    "${it}"
                }
            }
        }
    }

    val suspendResolverSchema = KGraphQL.schema {
        repeat(1000) {
            query("automated-${it}") {
                suspendResolver { ->
                    delay(3)
                    "${it}"
                }
            }
        }
    }

    val query = "{ " + (0..999).map { "automated-${it}" }.joinToString(", ") + " }"

    @Test
    fun `1000 synchronous resolvers sleeping with Thread sleep`(){
        val map = deserialize(syncResolversSchema.execute(query))
        MatcherAssert.assertThat(map.extract<String>("data/automated-0"), CoreMatchers.equalTo("0"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-271"), CoreMatchers.equalTo("271"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-314"), CoreMatchers.equalTo("314"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-500"), CoreMatchers.equalTo("500"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-999"), CoreMatchers.equalTo("999"))
    }

    @Test
    fun `1000 suspending resolvers sleeping with suspending delay`(){
        val map = deserialize(suspendResolverSchema.execute(query))
        MatcherAssert.assertThat(map.extract<String>("data/automated-0"), CoreMatchers.equalTo("0"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-271"), CoreMatchers.equalTo("271"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-314"), CoreMatchers.equalTo("314"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-500"), CoreMatchers.equalTo("500"))
        MatcherAssert.assertThat(map.extract<String>("data/automated-999"), CoreMatchers.equalTo("999"))
    }
}