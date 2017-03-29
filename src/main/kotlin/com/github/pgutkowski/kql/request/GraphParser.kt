package com.github.pgutkowski.kql.request

import com.github.pgutkowski.kql.Graph


interface GraphParser {
    fun parse(input: String): Graph
}