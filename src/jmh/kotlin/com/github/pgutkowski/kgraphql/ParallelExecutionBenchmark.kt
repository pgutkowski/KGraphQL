package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.Schema
import org.junit.Test
import org.openjdk.jmh.annotations.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1)
open class ParallelExecutionBenchmark {

    @Param("true", "false")
    var withSuspendResolvers = false

    var schema : Schema = KGraphQL.schema {  }

    @Setup
    fun setup(){
        schema = KGraphQL.schema {

            if (withSuspendResolvers == false)
                repeat(1000) {
                    query("automated-${it}") {
                        resolver { ->
                            Thread.sleep(3)
                            "${it}"
                        }
                    }
                } else {
                repeat(1000) {
                    query("automated-${it}") {
                        suspendResolver { ->
                            delay(3)
                            "${it}"
                        }
                    }
                }
            }
        }
    }

    @Benchmark @BenchmarkMode(Mode.AverageTime)
    fun queryBenchmark(): String = schema.execute( "{ " + (0..999).map { "automated-${it}" }.joinToString(", ") + " }" )

    @Test
    fun benchmarkWithThreads(){
        setup()
        println(queryBenchmark())
    }

    @Test
    fun benchmarkWithSuspendResolvers(){
        withSuspendResolvers = true
        setup()
        println(queryBenchmark())
    }
}