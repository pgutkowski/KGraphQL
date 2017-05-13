package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper


class UnionPropertyDSL(block: UnionPropertyDSL.() -> Unit) : ItemDSL() {
    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<Any>

    lateinit var returnType : String

    fun resolver(function: () -> Any){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <R>resolver(function: (R) -> Any){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <R, E>resolver(function: (R, E) -> Any){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <R, E, W>resolver(function: (R, E, W) -> Any){
        functionWrapper = FunctionWrapper.on(function)
    }

    fun <R, E, W, Q>resolver(function: (R, E, W, Q) -> Any){
        functionWrapper = FunctionWrapper.on(function)
    }
}