package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.request.Arguments


open class ExecutionNode(
        val schemaNode: SchemaNode.Branch,
        val children: Collection<ExecutionNode>,
        val key : String,
        val alias: String? = null,
        val arguments : Arguments? = null
) {

    val aliasOrKey = alias ?: key

    class Operation<T>(
            val operationNode: SchemaNode.Operation<T>,
            children: Collection<ExecutionNode>,
            key : String,
            alias: String? = null,
            arguments : Arguments? = null
    ) : ExecutionNode(operationNode, children, key, alias, arguments)
}