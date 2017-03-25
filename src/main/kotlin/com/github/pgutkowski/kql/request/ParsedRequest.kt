package com.github.pgutkowski.kql.request


class ParsedRequest(val action : Action, map : MultiMap, val name : String = "") {
    enum class Action {
        QUERY, MUTATION
    }
}