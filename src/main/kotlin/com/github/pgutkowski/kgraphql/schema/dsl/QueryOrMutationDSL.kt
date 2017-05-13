package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper


class QueryOrMutationDSL(block : QueryOrMutationDSL.() -> Unit) : ItemDSL() {
    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<*>

    fun <T>resolver(function: () -> T){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <T, R>resolver(function: (R) -> T){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <T, R, E>resolver(function: (R, E) -> T){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <T, R, E, W>resolver(function: (R, E, W) -> T){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <T, R, E, W, Q>resolver(function: (R, E, W, Q) -> T){
        functionWrapper = FunctionWrapper.on(function)
    }
}