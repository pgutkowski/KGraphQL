package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.dropBraces
import com.github.pgutkowski.kgraphql.getCollectionElementType
import com.github.pgutkowski.kgraphql.isBraced
import com.github.pgutkowski.kgraphql.isCollection
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.structure.TypeDefinitionProvider
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

@Suppress("UNCHECKED_CAST")
data class Variables(private val typeNameProvider: TypeDefinitionProvider,
                     private val variablesJson: VariablesJson,
                     private val variables: List<OperationVariable>?) {

    private val cache : MutableMap<String, Any?> = Collections.synchronizedMap(mutableMapOf())

    /**
     * map and return object of requested class
     */
    fun <T : Any> get(kClass: KClass<T>, kType: KType, key: String, transform: (value: String, type: KClass<T>) -> Any?): T? {
        val returnValue = cache.getOrPut(key){
            val variable = variables?.find { key == it.name }
                    ?: throw IllegalArgumentException("Variable '$key' was not declared for this operation")

            val isCollection = kClass.isCollection()

            if(isCollection){
                validateCollectionVariable(kType, variable)
            } else {
                validateSimpleVariable(kClass, variable)
            }

            //remove "!", it only denotes non-nullability
            val value = variablesJson.get(kClass, kType, key.substring(1))

            value?.let {
                if (isCollection && !(kType.getCollectionElementType()?.isMarkedNullable ?: true)) {
                    for (element in value as Collection<*>) {
                        if (element == null) {
                            throw RequestException(
                                    "Invalid argument value $value from variable $key, " +
                                            "expected list with non null arguments"
                            )
                        }
                    }
                }
            }

            when {
                value != null -> value
                variable.defaultValue != null -> transformDefaultValue(transform, variable.defaultValue, kClass)
                else -> null
            }
        }
        return returnValue as T?
    }

    private fun validateCollectionVariable(kType: KType, variable: OperationVariable) {
        val elementClass = kType.getCollectionElementType()?.jvmErasure
                ?: throw ExecutionException("Failed to extract element class from kType $kType")

        val kqlType = typeNameProvider.inputTypeByKClass(elementClass)
                ?: throw RequestException("Unknown input type ${variable.type}. Maybe it was not registered in schema?")

        if(variable.type.isBraced()){
            validateDeclaredVariableType(kqlType, variable.type.dropBraces())
        } else {
            throw RequestException("Expected List type, found simple type")
        }
    }

    private fun <T : Any> validateSimpleVariable(kClass: KClass<T>, variable: OperationVariable) {
        val kqlType = typeNameProvider.inputTypeByKClass(kClass)
                ?: throw RequestException("Unknown input type ${variable.type}. Maybe it was not registered in schema?")

        validateDeclaredVariableType(kqlType, variable.type)
    }

    private fun validateDeclaredVariableType(kqlType: KQLType, type: String) {
        if (kqlType.name != type.removeSuffix("!")) {
            throw IllegalArgumentException("Invalid variable argument type $type, expected ${kqlType.name}")
        }
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