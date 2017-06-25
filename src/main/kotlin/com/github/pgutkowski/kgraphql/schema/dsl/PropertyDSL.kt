package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty


class PropertyDSL<T, R>(val name : String, block : PropertyDSL<T, R>.() -> Unit) : DepreciableItemDSL() {

    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<R>

    fun resolver(function: (T) -> R){
        functionWrapper = FunctionWrapper.on(function, true)
    }

    fun <E>resolver(function: (T, E) -> R){
        functionWrapper = FunctionWrapper.on(function, true)
    }

    fun <E, W>resolver(function: (T, E, W) -> R){
        functionWrapper = FunctionWrapper.on(function, true)
    }

    fun <E, W, Q>resolver(function: (T, E, W, Q) -> R){
        functionWrapper = FunctionWrapper.on(function, true)
    }

    fun toKQL(): KQLProperty.Function<R> {
        return KQLProperty.Function(
                name,
                functionWrapper,
                description,
                isDeprecated,
                deprecationReason
        )
    }
}