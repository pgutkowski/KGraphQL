package com.github.pgutkowski.kgraphql.schema.model

import kotlin.reflect.KProperty1

interface PropertyDef : Depreciable, DescribedDef {

    val name : String

    open class Function<T> (
            name : String,
            resolver: FunctionWrapper<T>,
            override val description: String? = null,
            override val isDeprecated: Boolean = false,
            override val deprecationReason: String? = null,
            inputValues : List<InputValueDef<*>> = emptyList()
    ) : BaseOperationDef<T>(name, resolver, inputValues), PropertyDef

    open class Kotlin<T, R> (
            val kProperty: KProperty1<T, R>,
            override val description: String? = null,
            override val isDeprecated: Boolean = false,
            override val deprecationReason: String? = null,
            val isIgnored : Boolean = false
    ) : Definition(kProperty.name), PropertyDef

    class Union (
            name : String,
            resolver : FunctionWrapper<Any?>,
            val union : TypeDef.Union,
            description: String?,
            isDeprecated: Boolean,
            deprecationReason: String?,
            inputValues : List<InputValueDef<*>>
    ) : Function<Any?>(name, resolver, description, isDeprecated, deprecationReason, inputValues), PropertyDef
}