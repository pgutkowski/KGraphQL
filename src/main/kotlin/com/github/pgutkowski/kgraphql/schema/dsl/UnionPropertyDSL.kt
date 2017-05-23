package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper


class UnionPropertyDSL<T>(block: UnionPropertyDSL<T>.() -> Unit) : ItemDSL() {
    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<Any?>

    lateinit var returnType : String

    fun resolver(function: (T) -> Any?){
        functionWrapper = FunctionWrapper.on(function, true)
    }

    fun <E>resolver(function: (T, E) -> Any?){
        functionWrapper = FunctionWrapper.on(function, true)
    }

    fun <E, W>resolver(function: (T, E, W) -> Any?){
        functionWrapper = FunctionWrapper.on(function, true)
    }

    fun <E, W, Q>resolver(function: (T, E, W, Q) -> Any?){
        functionWrapper = FunctionWrapper.on(function, true)
    }
}