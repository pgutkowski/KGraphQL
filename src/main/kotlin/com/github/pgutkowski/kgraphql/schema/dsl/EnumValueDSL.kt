package com.github.pgutkowski.kgraphql.schema.dsl


class EnumValueDSL<T : Enum<T>>(value : T, block : EnumValueDSL<T>.() -> Unit) : DepreciableItemDSL(){
    init {
        block()
    }
}