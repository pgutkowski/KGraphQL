package com.github.pgutkowski.kgraphql.schema.dsl

@SchemaBuilderMarker
abstract class ItemDSL {
    var description : String? = null

    lateinit open var name : String
}