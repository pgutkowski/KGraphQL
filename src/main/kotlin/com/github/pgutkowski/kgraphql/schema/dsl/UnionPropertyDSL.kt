package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType


class UnionPropertyDSL<T>(val name : String, block: UnionPropertyDSL<T>.() -> Unit) : DepreciableItemDSL() {
    init {
        block()
    }

    internal lateinit var functionWrapper : FunctionWrapper<Any?>

    lateinit var returnType : TypeID

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

    fun toKQL(union : KQLType.Union): KQLProperty.Union {
        return KQLProperty.Union(
                name = name,
                resolver = functionWrapper,
                union = union,
                description = description,
                isDeprecated = isDeprecated,
                deprecationReason = deprecationReason
        )
    }
}