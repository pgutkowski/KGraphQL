package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.InputValueDef
import com.github.pgutkowski.kgraphql.schema.model.PropertyDef


class PropertyDSL<out T, R>(val name : String, block : PropertyDSL<T, R>.() -> Unit) : DepreciableItemDSL(), ResolverDSL.Target {

    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<R>

    private val inputValues = mutableListOf<InputValueDef<*>>()

    private fun resolver(function: FunctionWrapper<R>): ResolverDSL {
        functionWrapper = function
        return ResolverDSL(this)
    }

    fun resolver(function: (T) -> R) = resolver(FunctionWrapper.on(function, true))

    fun <E>resolver(function: (T, E) -> R) = resolver(FunctionWrapper.on(function, true))

    fun <E, W>resolver(function: (T, E, W) -> R) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q>resolver(function: (T, E, W, Q) -> R) = resolver(FunctionWrapper.on(function, true))

    fun toKQL(): PropertyDef.Function<R> {
        return PropertyDef.Function(
                name,
                functionWrapper,
                description,
                isDeprecated,
                deprecationReason,
                inputValues
        )
    }

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }
}