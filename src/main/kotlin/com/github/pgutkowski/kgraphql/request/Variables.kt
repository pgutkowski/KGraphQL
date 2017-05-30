package com.github.pgutkowski.kgraphql.request

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlin.reflect.KClass

/**
 * Represents already parsed variables json
 */
class Variables(val json: JsonNode? = null){

    companion object {
        //TODO: make object mapper configurable by user
        private val objectMapper = ObjectMapper()
    }

    constructor(json : String) : this(objectMapper.readTree(json))

    /**
     * map and return object of requested class
     */
    fun <T : Any>get(kClass: KClass<T>, key : String) : T? {
        if(json != null){
            return objectMapper.treeToValue(json[key], kClass.java)
        } else return null
    }

    /**
     * reified generic wrapper for [get]
     * map and return object of requested class
     */
    inline fun <reified T : Any>get(key: String) : T? {
        return get(T::class, key)
    }
}