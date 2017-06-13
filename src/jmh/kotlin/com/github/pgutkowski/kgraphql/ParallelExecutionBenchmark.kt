package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.Schema
import org.junit.jupiter.api.Test
import org.openjdk.jmh.annotations.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit


@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 5)
open class ParallelExecutionBenchmark {

    var schema : Schema = KGraphQL.schema {  }

    @Setup
    fun setup(){
        schema = KGraphQL.schema {

            query("sample1") {
                resolver { ->
                    Thread.sleep(2)
                    "233"
                }
            }
            query("sample2") {
                resolver { ->
                    Thread.sleep(3)
                    3.14 * ThreadLocalRandom.current().nextInt()
                }
            }
            query("sample3") {
                resolver { ->
                    Thread.sleep(2)
                    ThreadLocalRandom.current().nextInt() / 21
                }
            }
        }
    }

    @Benchmark @BenchmarkMode(Mode.AverageTime)
    fun queryBenchmark(): String = schema.execute("{sample1, sample2, sample3}")

    @Test
    fun check(){
        setup()
        println(queryBenchmark())
    }

}