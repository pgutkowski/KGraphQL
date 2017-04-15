package com.github.pgutkowski.kgraphql.request

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlin.reflect.KClass

class Variables(val objectMapper: ObjectMapper,val json: JsonNode?){

    fun <T : Any>getVariable(kClass: KClass<T>, key : String) : T? {
        if(json != null){
            return objectMapper.treeToValue(json[key], kClass.java)
        } else return null
    }
}