package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import kotlin.reflect.KClass


class ScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T>.() -> Unit) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    init {
        block()
    }

    var serialize : ((String) -> T)? = null

    var deserialize : ((T) -> String)? = null

    var validate : ((String)-> Boolean)? = null
}