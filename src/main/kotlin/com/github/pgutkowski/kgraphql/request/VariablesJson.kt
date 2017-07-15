package com.github.pgutkowski.kgraphql.request

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.getIterableElementType
import com.github.pgutkowski.kgraphql.isIterable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

/**
 * Represents already parsed variables json
 */
interface VariablesJson{

    fun <T : Any>get(kClass: KClass<T>, kType: KType, key : String) : T?

    class Empty : VariablesJson {
        override fun <T : Any> get(kClass: KClass<T>, kType: KType, key: String): T? {
            return null
        }
    }

    class Defined(val objectMapper: ObjectMapper, val json: JsonNode) : VariablesJson {

        constructor(objectMapper: ObjectMapper, json : String) : this(objectMapper, objectMapper.readTree(json))

        /**
         * map and return object of requested class
         */
        override fun <T : Any>get(kClass: KClass<T>, kType: KType, key : String) : T? {
            if(kClass != kType.jvmErasure) throw IllegalArgumentException("kClass and KType must represent same class")
            return json.let { node ->  node[key] }?.let { tree ->
                try {
                    objectMapper.convertValue(tree, kType.toTypeReference())
                } catch(e : Exception){
                    throw ExecutionException("Failed to coerce $tree as $kType", e)
                }
            }
        }
    }

    fun KType.toTypeReference(): JavaType {
        if(jvmErasure.isIterable()){
            val elementType = getIterableElementType()
                    ?: throw ExecutionException("Cannot handle collection without element type")

            return TypeFactory.defaultInstance().constructCollectionType(List::class.java, elementType.jvmErasure.java)
        } else {
            return TypeFactory.defaultInstance().constructSimpleType(jvmErasure.java, emptyArray())
        }
    }
}