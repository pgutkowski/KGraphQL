package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery


class QueryOrMutationDSL(val name : String, block : QueryOrMutationDSL.() -> Unit) : DepreciableItemDSL() {
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

    internal fun toKQLQuery(): KQLQuery<out Any?> {
        return KQLQuery(name, functionWrapper, description, isDeprecated, deprecationReason)
    }

    internal fun toKQLMutation(): KQLMutation<out Any?> {
        return KQLMutation(name, functionWrapper, description, isDeprecated, deprecationReason)
    }
}