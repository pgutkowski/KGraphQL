package com.github.pgutkowski.kql.result

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.github.pgutkowski.kql.Graph
import com.github.pgutkowski.kql.schema.impl.DefaultSchema
import kotlin.reflect.full.memberProperties


class ResultSerializer(val schema: DefaultSchema) : JsonSerializer<Result>() {

    //assuming that Jackson will provide non null values for all arguments
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
                for((key, schema) in graphSchema){
                    writeProperty(key, valueMap[key], gen, serializers, schema)
                }
                gen.writeEndObject()
            } else {
                gen.writeObjectField("data", value.data)
            }
        }
        gen.writeEndObject()
    }

    private fun serialize(value: Any, gen: JsonGenerator, serializers: SerializerProvider, schema: Graph?){
        if(schema != null){
            serializeWithSchema(value, gen, serializers, schema)
        } else {
            serializeNoSchema(value, gen, serializers)
        }
    }

    private fun serializeNoSchema(value: Any, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value)
    }

    private fun serializeWithSchema(value: Any, gen: JsonGenerator, serializers: SerializerProvider, schema: Graph) {
        gen.writeStartObject()
        for((key, subSchema) in schema){
            val kProperty = value.javaClass.kotlin.memberProperties.find { it.name == key }
            if(kProperty == null) {
                serializers.reportMappingProblem("Cannot find property: $key on type: ${value.javaClass}")
            } else {
                writeProperty(key, kProperty.get(value) , gen, serializers, subSchema)
            }
        }
        gen.writeEndObject()
    }

    private fun writeProperty(key : String, actualValue: Any?, gen: JsonGenerator, serializers: SerializerProvider, schema: Any?) {
        gen.writeFieldName(key)
        if (actualValue == null) {
            gen.writeNull()
        } else {
            when (actualValue) {
                is String -> gen.writeString(actualValue)
                is Int -> gen.writeNumber(actualValue)
                is Float -> gen.writeNumber(actualValue)
                else -> {
                    if (schema is Graph || schema == null) {
                        serialize(actualValue, gen, serializers, schema as Graph?)
                    } else {
                        serializers.reportMappingProblem("JSON schema not matching expected type \'${Graph::class}?\'")
                    }
                }
            }
        }
    }
}