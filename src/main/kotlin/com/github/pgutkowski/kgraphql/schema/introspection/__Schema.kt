package com.github.pgutkowski.kgraphql.schema.introspection


interface __Schema {

    val types : List<__Type>

    val queryType : __Type

    val mutationType : __Type?

    val subscryptionType : __Type?
}