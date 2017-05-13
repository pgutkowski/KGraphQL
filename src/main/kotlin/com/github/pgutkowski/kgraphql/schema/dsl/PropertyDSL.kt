package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper


class PropertyDSL<T, R>(block : PropertyDSL<T, R>.() -> Unit) : ItemDSL() {

    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<R>

    fun resolver(function: (T) -> R){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <E>resolver(function: (T, E) -> R){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <E, W>resolver(function: (T, E, W) -> R){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <E, W, Q>resolver(function: (T, E, W, Q) -> R){
        functionWrapper = FunctionWrapper.on(function)
    }
}