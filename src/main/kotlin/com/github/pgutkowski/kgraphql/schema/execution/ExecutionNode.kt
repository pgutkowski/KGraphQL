package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode


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

    class Union (
            val unionNode : SchemaNode.UnionProperty,
            val memberChildren: Map<SchemaNode.ReturnType, Collection<ExecutionNode>>,
            key: String,
            alias: String? = null
    ) : ExecutionNode(unionNode, emptyList(), key, alias) {
        fun memberExecution(type: SchemaNode.ReturnType): ExecutionNode {
            return ExecutionNode(
                    schemaNode,
                    memberChildren[type] ?: throw IllegalArgumentException("Union ${unionNode.kqlProperty.name} has no member $type"),
                    key,
                    alias,
                    arguments
            )
        }
    }
}