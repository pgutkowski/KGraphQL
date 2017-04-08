package com.github.pgutkowski.kgraphql.request


class Request(val action : Action, val graph : Graph, val name : String = "") {
    enum class Action {
        QUERY, MUTATION
    }
}