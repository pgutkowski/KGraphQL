package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import kotlin.reflect.KClass


class EnumDSL<T : Enum<T>>(kClass: KClass<T>, block : (EnumDSL<T>.() -> Unit)?) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    val valueDefinitions = mutableMapOf<T, EnumValueDSL<T>>()

    init {
        block?.invoke(this)
    }

    fun value(value : T, block : EnumValueDSL<T>.() -> Unit){
        valueDefinitions[value] = EnumValueDSL(value, block)
    }

    infix fun T.describe(content: String){
        valueDefinitions[this] = EnumValueDSL(this){
            description = content
        }
    }

}