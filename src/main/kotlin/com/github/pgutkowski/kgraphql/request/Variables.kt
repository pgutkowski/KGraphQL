package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.typeName
import kotlin.reflect.KClass


data class Variables(private val variablesJson: VariablesJson, private val variables: List<Variable>?){
    /**
     * map and return object of requested class
     */
    fun <T : Any>get(kClass: KClass<T>, key : String, transform: (value: String, type: KClass<T>)-> Any?) : T? {
        val variable = variables?.find { key == it.name }
                ?: throw IllegalArgumentException("Variable '$key' was not declared for this operation")
        //remove "!", it only denotes non-nullability
        if(kClass.typeName() != variable.type.removeSuffix("!")){
            throw IllegalArgumentException("Invalid variable argument type ${variable.type}, expected ${kClass.typeName()}")
        }
        val value = variablesJson.get(kClass, key.substring(1))
        when {
            value != null -> return value
            variable.defaultValue != null -> return transformDefaultValue(transform, variable.defaultValue, kClass)
            else -> return null
        }
    }

    private fun <T : Any> transformDefaultValue(transform: (value: String, type: KClass<T>) -> Any?, defaultValue: String, kClass: KClass<T>): T? {
        val defaultValue = transform.invoke(defaultValue, kClass)
        when {
            defaultValue == null -> return null
            kClass.isInstance(defaultValue) -> return defaultValue as T?
            else -> {
                throw ExecutionException("Invalid transform function returned ")
            }
        }
    }
}