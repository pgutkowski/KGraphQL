package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import kotlin.reflect.KClass


class InputTypeDSL<T: Any>(val kClass: KClass<T>, block: (InputTypeDSL<T>.() -> Unit)?) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    init {
        block?.invoke(this)
    }
}