package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.graph.Graph


data class Operation(val graph : Graph, val variables: List<Variable>?, val name : String?, val action : Action?) {

    enum class Action {
        QUERY, MUTATION;

        companion object {
            fun parse(input : String?): Action? {
                if(input == null){
                    return null
                } else {
                    return Action.valueOf(input.toUpperCase())
                }
            }
        }
    }
}