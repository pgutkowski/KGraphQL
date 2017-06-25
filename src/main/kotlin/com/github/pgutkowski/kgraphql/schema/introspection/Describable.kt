package com.github.pgutkowski.kgraphql.schema.introspection


interface Describable {
    val name : String

    val description : String?
}