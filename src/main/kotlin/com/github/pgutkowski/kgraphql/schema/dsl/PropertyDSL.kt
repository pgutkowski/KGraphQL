package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.InputValueDef
import com.github.pgutkowski.kgraphql.schema.model.PropertyDef


class PropertyDSL<T, R>(val name : String, block : PropertyDSL<T, R>.() -> Unit) : DepreciableItemDSL(), ResolverDSL.Target {

    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<R>

    private val inputValues = mutableListOf<InputValueDef<*>>()

    fun resolver(function: (T) -> R): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function, true)
        return ResolverDSL(this)
    }

    fun <E>resolver(function: (T, E) -> R): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function, true)
        return ResolverDSL(this)
    }

    fun <E, W>resolver(function: (T, E, W) -> R): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function, true)
        return ResolverDSL(this)
    }

    fun <E, W, Q>resolver(function: (T, E, W, Q) -> R): ResolverDSL {
        functionWrapper = FunctionWrapper.on(function, true)
        return ResolverDSL(this)
    }

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