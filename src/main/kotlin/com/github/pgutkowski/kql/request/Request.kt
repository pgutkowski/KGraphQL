package com.github.pgutkowski.kql.request

import com.github.pgutkowski.kql.Graph


class Request(val action : Action, val graph : Graph, val name : String = "") {
    enum class Action {
        QUERY, MUTATION
    }
}