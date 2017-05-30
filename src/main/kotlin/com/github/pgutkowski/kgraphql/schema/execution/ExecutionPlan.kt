package com.github.pgutkowski.kgraphql.schema.execution

class ExecutionPlan(val c: List<ExecutionNode.Operation<*>>) : List<ExecutionNode.Operation<*>> by c