package com.github.pgutkowski.kql.schema.impl.function

import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters

abstract class FunctionWrapper<T>(val instance : T, val function : KFunction<*>){

    fun invoke(vararg args: Any?) : Any? {
        return function.call(instance, *args)
    }

    val arity = function.valueParameters.size
}