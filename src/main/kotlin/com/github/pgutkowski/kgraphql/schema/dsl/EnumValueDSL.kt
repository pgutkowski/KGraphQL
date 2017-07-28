package com.github.pgutkowski.kgraphql.schema.dsl


class EnumValueDSL<T : Enum<T>>(val value : T, block : EnumValueDSL<T>.() -> Unit) : DepreciableItemDSL(){
    init {
        block()
    }
}