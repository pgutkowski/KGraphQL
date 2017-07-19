package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.InputValueDef
import com.github.pgutkowski.kgraphql.schema.model.PropertyDef
import com.github.pgutkowski.kgraphql.schema.model.TypeDef


class UnionPropertyDSL<T>(val name : String, block: UnionPropertyDSL<T>.() -> Unit) : DepreciableItemDSL(), ResolverDSL.Target {

    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<Any?>

    lateinit var returnType : TypeID

    private val inputValues = mutableListOf<InputValueDef<*>>()

    private fun resolver(function: FunctionWrapper<Any?>): ResolverDSL {
        functionWrapper = function
        return ResolverDSL(this)
    }

    fun resolver(function: (T) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E>resolver(function: (T, E) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W>resolver(function: (T, E, W) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q>resolver(function: (T, E, W, Q) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun toKQL(union : TypeDef.Union): PropertyDef.Union {
        return PropertyDef.Union (
                name = name,
                resolver = functionWrapper,
                union = union,
                description = description,
                isDeprecated = isDeprecated,
                deprecationReason = deprecationReason,
                inputValues = inputValues
        )
    }

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }
}