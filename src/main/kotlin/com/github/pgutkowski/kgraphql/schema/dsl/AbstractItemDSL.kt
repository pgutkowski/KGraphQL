package com.github.pgutkowski.kgraphql.schema.dsl


abstract class AbstractItemDSL {
    var description : String? = null

    lateinit open var name : String
}