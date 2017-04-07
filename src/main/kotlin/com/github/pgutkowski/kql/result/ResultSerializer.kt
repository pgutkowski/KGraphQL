package com.github.pgutkowski.kql.result

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.github.pgutkowski.kql.request.Graph
import com.github.pgutkowski.kql.TypeException
import com.github.pgutkowski.kql.request.GraphNode
import com.github.pgutkowski.kql.schema.impl.DefaultSchema
import com.github.pgutkowski.kql.schema.impl.KQLObject
import com.sun.javaws.exceptions.InvalidArgumentException
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
                val graphSchema = value.request.graph
                val valueMap = value.data ?: throw IllegalArgumentException("Cannot serialize null data")
                for(node in graphSchema){
                    writeProperty(valueMap[node.key], gen, serializers, node)
                }
                gen.writeEndObject()
            } else {
                serializers.reportMappingProblem("Cannot execute serialization without request")
            }
        }
        gen.writeEndObject()
    }

    //TODO: investigate how jackson internal serializers are implemented of improve this implementation
    private fun serialize(value: Any, gen: JsonGenerator, serializers: SerializerProvider, schema: Graph?){
        if(schema != null){
            gen.writeStartObject()
            for(node in schema){

                val kProperty = value.javaClass.kotlin.memberProperties.find { it.name == node.key }
                if(kProperty == null) {
                    serializers.reportMappingProblem("Cannot find property: ${node.key} on type: ${value.javaClass}")
                } else {
                    writeProperty(kProperty.get(value) , gen, serializers, node)
                }
            }
            gen.writeEndObject()
        } else {
            /**
             * Still not sure if serializing without schema should be executed at all.
             * Maybe there should at least be validation with schema,
             * so client code had of pass ALL classes in schema (?)
             */
            gen.writeObject(value)
        }
    }

    private fun writeProperty(actualValue: Any?, gen: JsonGenerator, serializers: SerializerProvider, graphNode: GraphNode) {
        gen.writeFieldName(graphNode.key)
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
                else -> {
                    writeComplexValue(actualValue, gen, serializers, graphNode)
                }
            }
        }
    }

    private fun writeComplexValue(actualValue: Any, gen: JsonGenerator, serializers: SerializerProvider, graphNode: GraphNode?) {
        val scalar = schema.scalars.find { it.kClass.isInstance(actualValue) }

        if(scalar != null){
            if(graphNode is GraphNode.Leaf){
                writeScalarValue(scalar, actualValue, gen, serializers)
            } else {
                serializers.reportMappingProblem("Cannot query properties on scalar types")
            }
        } else {
            when(graphNode){
                is GraphNode.Leaf -> serialize(actualValue, gen, serializers, null)
                is GraphNode.ToGraph -> serialize(actualValue, gen, serializers, graphNode.graph)
                //handle mutation result
                is GraphNode.ToArguments -> serialize(actualValue, gen, serializers, graphNode.graph)
                else -> serializers.reportMappingProblem("Cannot handle GraphNode of type ${graphNode?.javaClass}")
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