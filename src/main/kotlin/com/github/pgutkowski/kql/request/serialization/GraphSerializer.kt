package com.github.pgutkowski.kql.request.serialization

import com.github.pgutkowski.kql.request.Graph


interface GraphSerializer {
    fun serialize(graph : Graph) : String
}