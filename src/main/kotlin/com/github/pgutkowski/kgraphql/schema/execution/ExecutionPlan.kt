package com.github.pgutkowski.kgraphql.schema.execution

class ExecutionPlan(val c: List<Execution.Operation<*>>) : List<Execution.Operation<*>> by c