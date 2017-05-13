package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.valueParameters

abstract class BaseKQLOperation<T>(
        name : String,
        private val operationWrapper: FunctionWrapper<T>
) : KQLObject(name), KQLOperation<T>, FunctionWrapper<T> by operationWrapper {

    override val argumentsDescriptor = createArgumentsDescriptor()

    private fun createArgumentsDescriptor(): Map<String, KType> {
        val arguments : MutableMap<String, KType> = mutableMapOf()
        operationWrapper.kFunction.valueParameters.forEach { parameter ->
            if(parameter.name == null){
                throw SchemaException("Cannot handle nameless argument on function: ${operationWrapper.kFunction}")
            }
            arguments[parameter.name!!] = parameter.type
        }
        return arguments
    }
}