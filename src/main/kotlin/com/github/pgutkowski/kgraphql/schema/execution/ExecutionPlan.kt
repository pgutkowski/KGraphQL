package com.github.pgutkowski.kgraphql.schema.execution

class ExecutionPlan (
        val operations: List<Execution.Operation<*>>
) : List<Execution.Operation<*>> by operations