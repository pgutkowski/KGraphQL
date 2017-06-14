package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.structure.validateName
import kotlin.reflect.KType

abstract class BaseKQLOperation<T>(
        name : String,
        private val operationWrapper: FunctionWrapper<T>
) : KQLObject(name), KQLOperation<T>, FunctionWrapper<T> by operationWrapper {

    override val argumentsDescriptor = createArgumentsDescriptor()

    private fun createArgumentsDescriptor(): Map<String, KType> {
        val arguments : MutableMap<String, KType> = mutableMapOf()
        valueParameters().associateTo(arguments) { parameter ->
            val parameterName = parameter.name
                    ?: throw SchemaException("Cannot handle nameless argument on function: ${operationWrapper.kFunction}")

            validateName(parameterName)
            parameterName to parameter.type
        }
        return arguments
    }
}