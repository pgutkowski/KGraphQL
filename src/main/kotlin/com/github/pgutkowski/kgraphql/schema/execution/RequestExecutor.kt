package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.request.VariablesJson


interface RequestExecutor<Context> {
    fun execute(plan : ExecutionPlan, variables: VariablesJson, context: Context?) : String
}