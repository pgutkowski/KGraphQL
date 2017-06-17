package com.github.pgutkowski.kgraphql

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.BenchmarkSchema.oneResolver
import com.github.pgutkowski.kgraphql.BenchmarkSchema.threeResolver
import com.github.pgutkowski.kgraphql.BenchmarkSchema.twoResolver
import com.github.pgutkowski.kgraphql.schema.Schema
import org.junit.Test
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Warmup(iterations = 15)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 5)
open class SimpleExecutionOverheadBenchmark {

    @Param("true", "false")
    var withKGraphQL = true

    lateinit var schema : Schema

    lateinit var objectMapper : ObjectMapper

    @Setup
    fun setup(){
        if(withKGraphQL){
            schema = BenchmarkSchema.create {}
        } else {
            objectMapper = jacksonObjectMapper()
        }
    }

    @Benchmark
    fun benchmark(): String {
        if(withKGraphQL){
            return schema.execute("{one{name, quantity, active}, two(name : \"FELLA\"){range{start, endInclusive}}, three{id}}")
        } else {
            return ": ${objectMapper.writeValueAsString(oneResolver())} " +
                    ": ${objectMapper.writeValueAsString(twoResolver("FELLA"))} " +
                    ": ${objectMapper.writeValueAsString(threeResolver())}"
        }
    }

    @Test
    fun testWithKGraphQL(){
        setup()
        println(benchmark())
    }

    @Test
    fun testNoKGraphQL(){
        withKGraphQL = false
        println(benchmark())
    }
}
