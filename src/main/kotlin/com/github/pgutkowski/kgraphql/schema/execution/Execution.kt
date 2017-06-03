package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode


sealed class Execution {

    open class Node (
            val schemaNode: SchemaNode.Branch,
            val children: Collection<Execution>,
            val key : String,
            val alias: String? = null,
            val arguments : Arguments? = null,
            val condition : Condition? = null
    ) : Execution() {
        val aliasOrKey = alias ?: key
    }

    class Container(
            val condition: Condition,
            val elements : List<Execution.Node>
    ) : Execution()

    class Operation<T>(
            val operationNode: SchemaNode.Operation<T>,
            children: Collection<Execution>,
            key : String,
            alias: String? = null,
            arguments : Arguments? = null,
            condition : Condition? = null
    ) : Execution.Node(operationNode, children, key, alias, arguments, condition)

    class Union (
            val unionNode : SchemaNode.UnionProperty,
            val memberChildren: Map<SchemaNode.ReturnType, Collection<Execution>>,
            key: String,
            alias: String? = null,
            condition : Condition? = null
    ) : Execution.Node (unionNode, emptyList(), key, alias, null, condition) {
        fun memberExecution(type: SchemaNode.ReturnType): Execution.Node {
            return Execution.Node (
                    schemaNode,
                    memberChildren[type] ?: throw IllegalArgumentException("Union ${unionNode.kqlProperty.name} has no member $type"),
                    key,
                    alias,
                    arguments,
                    condition
            )
        }
    }
}