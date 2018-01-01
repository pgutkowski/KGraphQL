package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.Context
import kotlin.reflect.KProperty1

interface PropertyDef<T> : Depreciable, DescribedDef {

    val accessRule : ((T?, Context) -> Exception?)?

    val name : String

    open class Function<T, R> (
            name : String,
            resolver: FunctionWrapper<R>,
            override val description: String? = null,
            override val isDeprecated: Boolean = false,
            override val deprecationReason: String? = null,
            accessRule : ((T?, Context) -> Exception?)? = null,
            inputValues : List<InputValueDef<*>> = emptyList()
    ) : BaseOperationDef<T, R>(name, resolver, inputValues, accessRule), PropertyDef<T>

    open class Kotlin<T : Any, R> (
            val kProperty: KProperty1<T, R>,
            override val description: String? = null,
            override val isDeprecated: Boolean = false,
            override val deprecationReason: String? = null,
            override val accessRule : ((T?, Context) -> Exception?)? = null,
            val isIgnored : Boolean = false
    ) : Definition(kProperty.name), PropertyDef<T>

    class Union<T> (
            name : String,
            resolver : FunctionWrapper<Any?>,
            val union : TypeDef.Union,
            description: String?,
            isDeprecated: Boolean,
            deprecationReason: String?,
            accessRule : ((T?, Context) -> Exception?)? = null,
            inputValues : List<InputValueDef<*>>
    ) : Function<T, Any?>(name, resolver, description, isDeprecated, deprecationReason, accessRule, inputValues), PropertyDef<T>
}