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

    fun <T>resolver(function: () -> T): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function)
        return ResolverDSL(this)
    }

    fun <T, R>resolver(function: (R) -> T): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function)
        return ResolverDSL(this)
    }

    fun <T, R, E>resolver(function: (R, E) -> T): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function)
        return ResolverDSL(this)
    }

    fun <T, R, E, W>resolver(function: (R, E, W) -> T): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function)
        return ResolverDSL(this)
    }

    fun <T, R, E, W, Q>resolver(function: (R, E, W, Q) -> T): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function)
        return ResolverDSL(this)
    }

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