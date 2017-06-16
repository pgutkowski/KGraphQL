package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.request.graph.SelectionTree


data class Operation(val selectionTree: SelectionTree, val variables: List<OperationVariable>?, val name : String?, val action : Action?) {

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