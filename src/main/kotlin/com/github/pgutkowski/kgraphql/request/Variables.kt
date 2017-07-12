package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.getCollectionElementType
import com.github.pgutkowski.kgraphql.isCollection
import com.github.pgutkowski.kgraphql.schema.structure2.LookupSchema
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
data class Variables(
        private val typeDefinitionProvider: LookupSchema,
        private val variablesJson: VariablesJson,
        private val variables: List<OperationVariable>?
) {

    /**
     * map and return object of requested class
     */
    fun <T : Any> get(kClass: KClass<T>, kType: KType, key: String, transform: (value: String) -> Any?): T? {
        val variable = variables?.find { key == it.name }
                ?: throw IllegalArgumentException("Variable '$key' was not declared for this operation")

        val isCollection = kClass.isCollection()

        validateVariable(typeDefinitionProvider.typeReference(kType), variable)

        var value = variablesJson.get(kClass, kType, key.substring(1))
        if(value == null && variable.defaultValue != null){
            value = transformDefaultValue(transform, variable.defaultValue, kClass)
        }

        value?.let {
            if (isCollection && !(kType.getCollectionElementType()?.isMarkedNullable ?: true)) {
                for (element in value as Collection<*>) {
                    if (element == null) {
                        throw RequestException(
                                "Invalid argument value $value from variable $key, expected list with non null arguments"
                        )
                    }
                }
            }
        }

        return value
    }

    private fun <T : Any> transformDefaultValue(transform: (value: String) -> Any?, defaultValue: String, kClass: KClass<T>): T? {
        val transformedDefaultValue = transform.invoke(defaultValue)
        when {
            transformedDefaultValue == null -> return null
            kClass.isInstance(transformedDefaultValue) -> return transformedDefaultValue as T?
            else -> {
                throw ExecutionException("Invalid transform function returned ")
            }
        }
    }

    fun validateVariable(expectedType: TypeReference, variable: OperationVariable){
        val variableType = variable.type
        val invalidName =  expectedType.name != variableType.name
        val invalidIsList = expectedType.isList != variableType.isList
        val invalidNullability = !expectedType.isNullable && variableType.isNullable && variable.defaultValue == null
        val invalidElementNullability = !expectedType.isElementNullable && variableType.isElementNullable

        if(invalidName || invalidIsList || invalidNullability || invalidElementNullability){
            throw RequestException("Invalid variable ${variable.name} argument type $variableType, expected $expectedType")
        }
    }
}