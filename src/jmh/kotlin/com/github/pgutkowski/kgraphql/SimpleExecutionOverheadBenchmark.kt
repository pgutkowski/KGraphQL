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

    val ones = listOf(ModelOne("DUDE"),ModelOne("GUY"),ModelOne("PAL"),ModelOne("FELLA"))

    val oneResolver : ()->List<ModelOne> = { ones }

    val twoResolver : (name : String)-> ModelTwo? = { name ->
        ones.find { it.name == name }?.let { ModelTwo(it, it.quantity..12) }
    }

    val threeResolver : ()-> ModelThree = { ModelThree("", ones.map { ModelTwo(it, it.quantity..10) }) }

    lateinit var schema : Schema

    lateinit var objectMapper : ObjectMapper

    @Setup
    fun setup(){
        if(withKGraphQL){
            schema = KGraphQL.schema {
                query("one"){
                    resolver(oneResolver)
                }
                query("two"){
                    resolver(twoResolver)
                }
                query("three"){
                    resolver(threeResolver)
                }
            }
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
