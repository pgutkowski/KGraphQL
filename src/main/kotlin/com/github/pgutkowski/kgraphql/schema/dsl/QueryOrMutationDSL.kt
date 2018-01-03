package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.Context
import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.InputValueDef
import com.github.pgutkowski.kgraphql.schema.model.MutationDef
import com.github.pgutkowski.kgraphql.schema.model.QueryDef


class QueryOrMutationDSL(
        val name : String,
        block : QueryOrMutationDSL.() -> Unit
) : LimitedAccessItemDSL<Nothing>(), ResolverDSL.Target {

    private val inputValues = mutableListOf<InputValueDef<*>>()

    init {
        block()
    }

    internal var functionWrapper : FunctionWrapper<*>? = null

    private fun resolver(function: FunctionWrapper<*>): ResolverDSL {
        functionWrapper = function
        return ResolverDSL(this)
    }

    fun <T>resolver(function: () -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R>resolver(function: (R) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E>resolver(function: (R, E) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W>resolver(function: (R, E ,W ) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q>resolver(function: (R, E, W, Q) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A>resolver(function: (R, E, W, Q, A) -> T) = resolver(FunctionWrapper.on(function))

    fun <T, R, E, W, Q, A, S>resolver(function: (R, E, W, Q, A, S) -> T) = resolver(FunctionWrapper.on(function))

    fun accessRule(rule: (Context) -> Exception?){
        val accessRuleAdapter: (Nothing?, Context) -> Exception? = { _, ctx -> rule(ctx) }
        this.accessRuleBlock = accessRuleAdapter
    }

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }

    internal fun toKQLQuery(): QueryDef<out Any?> {
        val function = functionWrapper ?: throw IllegalArgumentException("resolver has to be specified for query [$name]")

        return QueryDef (
                name = name,
                resolver = function,
                description = description,
                isDeprecated = isDeprecated,
                deprecationReason = deprecationReason,
                inputValues = inputValues,
                accessRule = accessRuleBlock
        )
    }

    internal fun toKQLMutation(): MutationDef<out Any?> {
        val function = functionWrapper ?: throw IllegalArgumentException("resolver has to be specified for mutation [$name]")

        return MutationDef(
                name = name,
                resolver = function,
                description = description,
                isDeprecated = isDeprecated,
                deprecationReason = deprecationReason,
                inputValues = inputValues,
                accessRule = accessRuleBlock
        )
    }
}