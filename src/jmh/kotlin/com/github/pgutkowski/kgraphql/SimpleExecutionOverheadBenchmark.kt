package com.github.pgutkowski.kgraphql

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.schema.Schema
import org.junit.Test
import org.openjdk.jmh.annotations.*
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
            schema = Benchmark.benchmarkSchema{}
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
