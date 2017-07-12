package com.github.pgutkowski.kgraphql.schema.introspection

import com.github.pgutkowski.kgraphql.schema.model.Depreciable


interface __Field : Depreciable, __Described {

    val type: __Type

    val args: List<__InputValue>
}