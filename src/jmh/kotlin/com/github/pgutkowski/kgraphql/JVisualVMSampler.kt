package com.github.pgutkowski.kgraphql


val ones = listOf(ModelOne("DUDE"),ModelOne("GUY"),ModelOne("PAL"),ModelOne("FELLA"))

val oneResolver : ()->List<ModelOne> = { ones }

val twoResolver : (name : String)-> ModelTwo? = { name ->
    ones.find { it.name == name }?.let { ModelTwo(it, it.quantity..12) }
}

val threeResolver : () -> ModelThree = { ModelThree("", ones.map { ModelTwo(it, it.quantity..10) }) }

val schema = KGraphQL.schema {
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

fun main(vararg args: String){
    while(true){
        println(schema.execute("{one{name, quantity, active}}"))
        println(schema.execute("{two(name : \"FELLA\"){range{start, endInclusive}}}"))
        println(schema.execute("{three{id}}"))
    }
}