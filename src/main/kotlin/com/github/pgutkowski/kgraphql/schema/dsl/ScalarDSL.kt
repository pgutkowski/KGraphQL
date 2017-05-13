package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.typeName
import kotlin.reflect.KClass


class ScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T>.() -> Unit) : ItemDSL() {

    override var name = kClass.typeName()

    init {
        block()
    }

    var serialize : ((String) -> T)? = null

    var deserialize : ((T) -> String)? = null

    var validate : ((String)-> Boolean)? = null
}