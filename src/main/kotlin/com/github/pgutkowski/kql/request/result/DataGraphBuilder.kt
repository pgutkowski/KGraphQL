package com.github.pgutkowski.kql.request.result

import com.github.pgutkowski.kql.request.Graph
import com.github.pgutkowski.kql.request.SyntaxException
import com.github.pgutkowski.kql.schema.impl.DefaultSchema
import kotlin.reflect.full.memberProperties


class DataGraphBuilder(val schema : DefaultSchema) {
    fun from (input : Any, graphSchema: Graph? = null) : Graph {
        if(graphSchema != null){
            return buildFromSchema(input, graphSchema)
        } else {
            return buildAll(input)
        }
    }

    private fun buildAll(input: Any): Graph {
        val graph = Graph()
        input.javaClass.kotlin.memberProperties.forEach {
            graph.put(it.name, it.get(input))
        }
        return graph
    }

    private fun buildFromSchema(input: Any, graphSchema: Graph): Graph {
        val graph = Graph()
        for((key, value) in graphSchema){
            val property = input.javaClass.kotlin.memberProperties.find { it.name == key }
            if(property != null) {
                graph.put(property.name, property.get(input))
            } else {
                throw SyntaxException("field $key does not exist")
            }
        }
        return graph
    }

    fun String.isSimpleField() = !isCollectionField() && matches(Regex.fromLiteral("^[a-zA-Z0-9]*$"))

    fun String.isCollectionField() = startsWith('[') && endsWith(']')

    fun String.isNonNullableField() = endsWith('!')
}