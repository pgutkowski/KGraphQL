package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.typeName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


open class TypeDSL<T : Any>(kClass: KClass<T>, block: TypeDSL<T>.() -> Unit) : AbstractItemDSL() {

    override var name = kClass.typeName()

    internal val ignoredProperties = mutableSetOf<KProperty<*>>()

    fun ignore(kProperty: KProperty<*>){
        ignoredProperties.add(kProperty)
    }

    init {
        block()
    }
}