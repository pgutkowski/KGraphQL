package com.github.pgutkowski.kql.request.serialization

import com.github.pgutkowski.kql.request.Graph


interface GraphDeserializer {
    fun deserialize(input: String): Graph
}