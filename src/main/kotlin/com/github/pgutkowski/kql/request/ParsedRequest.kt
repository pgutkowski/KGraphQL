package com.github.pgutkowski.kql.request


class ParsedRequest(val action : Action, val graph : Graph, val name : String = "") {
    enum class Action {
        QUERY, MUTATION
    }
}