package com.github.pgutkowski.kgraphql.result

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.github.pgutkowski.kgraphql.TypeException
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.GraphNode
import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.impl.KQLObject
import kotlin.reflect.full.memberProperties


class ResultSerializer(val schema: DefaultSchema) : JsonSerializer<Result>() {

    override fun serialize(value: Result, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        if(value.errors != null){
            gen.writeObjectField("errors", value.errors)
        } else {
            if(value.request != null){
                gen.writeFieldName("data")
                gen.writeStartObject()
                val graphSchema = value.graph
                val valueMap = value.data ?: throw IllegalArgumentException("Cannot serialize null data")
                for(node in graphSchema){
                    writeProperty(valueMap[node.aliasOrKey], gen, serializers, node)
                }
                gen.writeEndObject()
            } else {
                serializers.reportMappingProblem("Cannot execute serialization without request")
            }
        }
        gen.writeEndObject()
    }

    //TODO: investigate how jackson internal serializers are implemented to check if this implementation can be improved
    private fun serialize(value: Any, gen: JsonGenerator, serializers: SerializerProvider, graph: Graph?){

        val schemaGraph = graph ?: schema.descriptor.get(value.javaClass.kotlin) ?: throw Exception("Cannot handle serialization of value : $value")

        gen.writeStartObject()
        for(node in schemaGraph){

            val kProperty = value.javaClass.kotlin.memberProperties.find { it.name == node.key }
            if(kProperty == null) {
                serializers.reportMappingProblem("Cannot find property: ${node.key} on type: ${value.javaClass}")
            } else {
                writeProperty(kProperty.get(value) , gen, serializers, node)
            }
        }
        gen.writeEndObject()
    }

    private fun writeProperty(actualValue: Any?, gen: JsonGenerator, serializers: SerializerProvider, graphNode: GraphNode) {
        gen.writeFieldName(graphNode.aliasOrKey)
        writeValue(actualValue, gen, serializers, graphNode)
    }

    private fun writeValue(actualValue: Any?, gen: JsonGenerator, serializers: SerializerProvider, graphNode: GraphNode) {
        if (actualValue == null) {
            gen.writeNull()
        } else {
            when (actualValue) {
                is String -> gen.writeString(actualValue)
                is Int -> gen.writeNumber(actualValue)
                is Float -> gen.writeNumber(actualValue)
                is Map<*, *> -> throw TypeException("instances of ${Map::class} are not supported in GraphQL, see https://github.com/facebook/graphql/issues/101")
                is Collection<*> -> {
                    gen.writeStartArray(actualValue.size)
                    for(entry in actualValue){
                        writeValue(entry, gen, serializers, graphNode)
                    }
                    gen.writeEndArray()
                }
                is Enum<*> -> gen.writeString(actualValue.name)
                else -> {
                    writeComplexValue(actualValue, gen, serializers, graphNode)
                }
            }
        }
    }

    private fun writeComplexValue(actualValue: Any, gen: JsonGenerator, serializers: SerializerProvider, graphNode: GraphNode) {
        val scalar = schema.findScalarByInstance(actualValue)

        if(scalar != null){
            if(graphNode.isLeaf){
                writeScalarValue(scalar, actualValue, gen, serializers)
            } else {
                serializers.reportMappingProblem("Cannot query properties on scalar simpleTypes")
            }
        } else {
            when{
                graphNode.hasArguments -> serialize(actualValue, gen, serializers, graphNode.children)
                graphNode.isLeaf -> serialize(actualValue, gen, serializers, null)
                graphNode.isBranch-> serialize(actualValue, gen, serializers, graphNode.children)
                else -> serializers.reportMappingProblem("Cannot handle : $graphNode")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any>writeScalarValue(scalar: KQLObject.Scalar<T>, actualValue: Any, gen: JsonGenerator, serializers: SerializerProvider){
        try{
            gen.writeString(scalar.scalarSupport.deserialize(actualValue as T))
        } catch (e : Exception){
            serializers.reportMappingProblem(e, "Failed of serialize value: $actualValue")
        }
    }
}