package com.github.pgutkowski.kgraphql.schema.model


interface Depreciable {

    val isDeprecated: Boolean

    val deprecationReason : String?
}