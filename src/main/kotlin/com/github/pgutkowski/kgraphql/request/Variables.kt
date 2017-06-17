package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import com.github.pgutkowski.kgraphql.schema.structure.TypeDefinitionProvider
import java.util.*
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
data class Variables(private val typeNameProvider: TypeDefinitionProvider,
                     private val variablesJson: VariablesJson,
                     private val variables: List<OperationVariable>?) {

    private data class CacheKey(val kClass: KClass<*>, val variableKey: String)

    private val cache : MutableMap<CacheKey, Any?> = Collections.synchronizedMap(mutableMapOf())

    /**
     * map and return object of requested class
     */
    fun <T : Any> get(kClass: KClass<T>, key: String, transform: (value: String, type: KClass<T>) -> Any?): T? {
        val returnValue = cache.getOrPut(CacheKey(kClass, key)){
            val variable = variables?.find { key == it.name }
                    ?: throw IllegalArgumentException("Variable '$key' was not declared for this operation")
            //remove "!", it only denotes non-nullability
            val kqlType = typeNameProvider.typeByKClass(kClass)
                    ?: throw RequestException("Unknown type ${variable.type}. Maybe it was not registered in schema?")
            if (kqlType.name != variable.type.removeSuffix("!")) {
                throw IllegalArgumentException("Invalid variable argument type ${variable.type}, expected ${kClass.defaultKQLTypeName()}")
            }
            val value = variablesJson.get(kClass, key.substring(1))

            when {
                value != null -> value
                variable.defaultValue != null -> transformDefaultValue(transform, variable.defaultValue, kClass)
                else -> null
            }
        }
        return returnValue as T?
    }

    private fun <T : Any> transformDefaultValue(transform: (value: String, type: KClass<T>) -> Any?, defaultValue: String, kClass: KClass<T>): T? {
        val transformedDefaultValue = transform.invoke(defaultValue, kClass)
        when {
            transformedDefaultValue == null -> return null
            kClass.isInstance(transformedDefaultValue) -> return transformedDefaultValue as T?
            else -> {
                throw ExecutionException("Invalid transform function returned ")
            }
        }
    }
}