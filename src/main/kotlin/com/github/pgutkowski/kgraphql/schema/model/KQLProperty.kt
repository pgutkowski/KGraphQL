package com.github.pgutkowski.kgraphql.schema.model

import kotlin.reflect.KProperty1

interface KQLProperty : Depreciable, DescribedKQLObject {

    val name : String

    open class Function<T> (
            name : String,
            resolver: FunctionWrapper<T>,
            override val description: String? = null,
            override val isDeprecated: Boolean = false,
            override val deprecationReason: String? = null
    ) : BaseKQLOperation<T>(name, resolver), KQLProperty

    open class Kotlin<T, R> (
            val kProperty: KProperty1<T, R>,
            override val description: String? = null,
            override val isDeprecated: Boolean = false,
            override val deprecationReason: String? = null,
            val isIgnored : Boolean = false
    ) : KQLObject(kProperty.name), KQLProperty

    class Union(
            name : String,
            resolver : FunctionWrapper<Any?>,
            val union : KQLType.Union,
            description: String?,
            isDeprecated: Boolean,
            deprecationReason: String?
    ) : Function<Any?>(name, resolver, description, isDeprecated, deprecationReason), KQLProperty
}