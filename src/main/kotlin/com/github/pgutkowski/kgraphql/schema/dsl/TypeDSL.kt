package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.typeName
import kotlin.reflect.KClass


open class TypeDSL<T : Any>(kClass: KClass<T>, init: (TypeDSL<T>.() -> Unit)?) : AbstractItemDSL() {

    val name = kClass.typeName()

    init {
        init?.invoke(this)
    }
}