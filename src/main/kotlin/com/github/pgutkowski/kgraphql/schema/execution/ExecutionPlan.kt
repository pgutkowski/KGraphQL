package com.github.pgutkowski.kgraphql.schema.execution

class ExecutionPlan (
        val operations: List<Execution.Node>
) : List<Execution.Node> by operations