package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.Context
import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.InputValueDef
import com.github.pgutkowski.kgraphql.schema.model.PropertyDef
import com.github.pgutkowski.kgraphql.schema.model.TypeDef
import java.lang.IllegalArgumentException


class UnionPropertyDSL<T : Any>(val name : String, block: UnionPropertyDSL<T>.() -> Unit) : LimitedAccessItemDSL<T>(), ResolverDSL.Target {

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

    fun <E, W, Q, A>resolver(function: (T, E, W, Q, A) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun <E, W, Q, A, S>resolver(function: (T, E, W, Q, A, S) -> Any?) = resolver(FunctionWrapper.on(function, true))

    fun accessRule(rule: (T, Context) -> Exception?){

        val accessRuleAdapter: (T?, Context) -> Exception? = { parent, ctx ->
            if (parent != null) rule(parent, ctx) else IllegalArgumentException("Unexpected null parent of kotlin property")
        }

        this.accessRuleBlock = accessRuleAdapter
    }

    fun toKQLProperty(union : TypeDef.Union) = PropertyDef.Union<T> (
            name = name,
            resolver = functionWrapper,
            union = union,
            description = description,
            isDeprecated = isDeprecated,
            deprecationReason = deprecationReason,
            inputValues = inputValues,
            accessRule = accessRuleBlock
    )

    override fun addInputValues(inputValues: Collection<InputValueDef<*>>) {
        this.inputValues.addAll(inputValues)
    }
}