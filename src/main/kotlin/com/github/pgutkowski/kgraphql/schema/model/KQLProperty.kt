package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper
import kotlin.reflect.KProperty1

interface KQLProperty {
    val name : String

    open class Function<T> (
            name : String,
            resolver: FunctionWrapper<T>
    ) : BaseKQLOperation<T>(name, resolver), KQLProperty

    open class Kotlin<T, R> (
            val kProperty: KProperty1<T, R>
    ) : KQLObject(kProperty.name), KQLProperty

    class Union(
            name : String,
            resolver : FunctionWrapper<Any?>,
            val union : KQLType.Union
    ) : Function<Any?>(name, resolver), KQLProperty
}