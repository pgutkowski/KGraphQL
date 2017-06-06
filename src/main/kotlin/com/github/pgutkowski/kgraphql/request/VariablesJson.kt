package com.github.pgutkowski.kgraphql.request

import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlin.reflect.KClass

/**
 * Represents already parsed variables json
 */
class VariablesJson(val json: JsonNode? = null){

    companion object {
        //TODO: make object mapper configurable by user
        private val objectMapper = ObjectMapper().registerKotlinModule()
    }

    constructor(json : String) : this(objectMapper.readTree(json))

    /**
     * map and return object of requested class
     */
    fun <T : Any>get(kClass: KClass<T>, key : String) : T? {
        return json?.let { node ->  node[key] }?.let { tree -> objectMapper.treeToValue(tree, kClass.java) }
    }

    /**
     * reified generic wrapper for [get]
     * map and return object of requested class
     */
    inline fun <reified T : Any>get(key: String) : T? {
        return get(T::class, key)
    }
}