package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import kotlin.reflect.KProperty1


class KotlinPropertyDSL<T>(val kProperty: KProperty1<T, *>, block : KotlinPropertyDSL<T>.() -> Unit) : DepreciableItemDSL(){

    var ignore = false

    init {
        block()
    }

    fun toKQLProperty(): KQLProperty.Kotlin<T, Any?> {
        return KQLProperty.Kotlin(kProperty, description, isDeprecated, deprecationReason, ignore)
    }
}