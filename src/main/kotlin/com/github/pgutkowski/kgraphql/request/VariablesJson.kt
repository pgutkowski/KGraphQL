package com.github.pgutkowski.kgraphql.request

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass

/**
 * Represents already parsed variables json
 */
interface VariablesJson{

    fun <T : Any>get(kClass: KClass<T>, key : String) : T?

    class Empty : VariablesJson {
        override fun <T : Any> get(kClass: KClass<T>, key: String): T? {
            return null
        }
    }

    class Defined(val objectMapper: ObjectMapper, val json: JsonNode? = null) : VariablesJson {

        constructor(objectMapper: ObjectMapper, json : String) : this(objectMapper, objectMapper.readTree(json))

        /**
         * map and return object of requested class
         */
        override fun <T : Any>get(kClass: KClass<T>, key : String) : T? {
            return json?.let { node ->  node[key] }?.let { tree -> objectMapper.treeToValue(tree, kClass.java) }
        }
    }
}