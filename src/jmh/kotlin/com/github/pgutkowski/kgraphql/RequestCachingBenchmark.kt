package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.Schema
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit


@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 5)
open class RequestCachingBenchmark {

    @Param("true", "false")
    var caching = true

    lateinit var schema : Schema

    @Setup
    fun setup(){
        schema = BenchmarkSchema.create {
            configure {
                useCachingDocumentParser = caching
            }
        }
    }

    @Benchmark
    fun benchmark() : String {
        return schema.execute("{one{name, quantity, active}, two(name : \"FELLA\"){range{start, endInclusive}}, three{id}}")
    }
}