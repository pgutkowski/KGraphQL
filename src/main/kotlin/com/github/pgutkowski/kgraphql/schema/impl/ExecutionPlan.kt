package com.github.pgutkowski.kgraphql.schema.impl


class ExecutionPlan(c: MutableCollection<out ExecutionNode.Operation<*>>?)  : ArrayList<ExecutionNode.Operation<*>>(c)