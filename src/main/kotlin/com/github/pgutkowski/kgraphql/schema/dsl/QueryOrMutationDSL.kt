package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.InputValueDef
import com.github.pgutkowski.kgraphql.schema.model.MutationDef
import com.github.pgutkowski.kgraphql.schema.model.QueryDef


class QueryOrMutationDSL(val name : String, block : QueryOrMutationDSL.() -> Unit) : DepreciableItemDSL(), ResolverDSL.Target {


    private val inputValues = mutableListOf<InputValueDef<*>>()

    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<*>

    private fun resolver(function: FunctionWrapper<*>): ResolverDSL {
        functionWrapper = function
        return ResolverDSL(this)
    }

    fun <T>resolver(function: () -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R>resolver(function: (R) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E>resolver(function: (R, E) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W>resolver(function: (R, E ,W ) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q>resolver(function: (R, E, W, Q) -> T) = resolver(FunctionWrapper.on(function))

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }

    internal fun toKQLQuery(): QueryDef<out Any?> {
        return QueryDef(name, functionWrapper, description, isDeprecated, deprecationReason, inputValues)
    }

    internal fun toKQLMutation(): MutationDef<out Any?> {
        return MutationDef(name, functionWrapper, description, isDeprecated, deprecationReason, inputValues)
    }
}