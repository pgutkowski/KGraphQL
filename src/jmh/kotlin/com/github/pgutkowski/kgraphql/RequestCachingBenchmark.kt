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
    var useParsedRequestCaching = true

    val ones = listOf(ModelOne("DUDE"), ModelOne("GUY"), ModelOne("PAL"), ModelOne("FELLA"))

    val oneResolver : () -> List<ModelOne> = { ones }

    val twoResolver : (name : String)-> ModelTwo? = { name ->
        ones.find { it.name == name }?.let { ModelTwo(it, it.quantity..12) }
    }

    val threeResolver : () -> ModelThree = { ModelThree("", ones.map { ModelTwo(it, it.quantity..10) }) }

    lateinit var schema : Schema

    @Setup
    fun setup(){
        schema = KGraphQL.schema {
            configure {
                cacheParsedRequests = useParsedRequestCaching
            }
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
    }

    @Benchmark
    fun benchmark() : String {
        return schema.execute("{one{name, quantity, active}, two(name : \"FELLA\"){range{start, endInclusive}}, three{id}}")
    }
}