package com.github.pgutkowski.kql

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kql.request.Arguments
import com.github.pgutkowski.kql.request.Graph
import com.github.pgutkowski.kql.request.GraphNode

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

fun leaf(key : String) = GraphNode.Leaf(key)

fun leafs(vararg keys : String): Array<GraphNode.Leaf> {
    return keys.map { GraphNode.Leaf(it) }.toTypedArray()
}

fun branch(key: String, vararg nodes: GraphNode) = GraphNode.ToGraph(key, Graph(*nodes))

fun argLeaf(key: String, args:  Arguments) = GraphNode.ToArguments(key, args)

fun args(vararg args: Pair<String, String>) = Arguments(*args)

fun argBranch(key: String, args: Arguments, vararg nodes: GraphNode): GraphNode.ToArguments {
    return GraphNode.ToArguments(key, args, if(nodes.isNotEmpty()) Graph(*nodes) else null)
}


