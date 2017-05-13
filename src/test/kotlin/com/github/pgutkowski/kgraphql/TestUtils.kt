package com.github.pgutkowski.kgraphql

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.GraphNode
import com.github.pgutkowski.kgraphql.schema.dsl.SchemaBuilder
import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchema

val objectMapper = jacksonObjectMapper()

fun deserialize(json: String) : Map<*,*> {
    return objectMapper.readValue(json, HashMap::class.java)
}

fun getMap(map : Map<*,*>, key : String) : Map<*,*>{
    return map[key] as Map<*,*>
}

@Suppress("UNCHECKED_CAST")
fun <T>extract(map: Map<*,*>, path : String) : T {
    val tokens = path.trim().split('/').filter(String::isNotBlank)
    try {
        return tokens.fold(map as Any?, { workingMap, token ->
            if(token.contains('[')){
                val list = (workingMap as Map<*,*>)[token.substringBefore('[')]
                val index = token[token.indexOf('[')+1].toString().toInt()
                (list as List<*>)[index]
            } else {
                (workingMap as Map<*,*>)[token]
            }
        }) as T
    } catch (e : Exception){
        throw IllegalArgumentException("Path: $path does not exist in map: $map", e)
    }
}

fun defaultSchema(block: SchemaBuilder.() -> Unit): DefaultSchema {
    return SchemaBuilder(block).build() as DefaultSchema
}


