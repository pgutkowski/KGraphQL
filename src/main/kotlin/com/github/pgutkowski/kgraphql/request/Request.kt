package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.graph.Graph


class Request(val action : Action, val graph : Graph, val name : String = "") {
    enum class Action {
        QUERY, MUTATION
    }
}