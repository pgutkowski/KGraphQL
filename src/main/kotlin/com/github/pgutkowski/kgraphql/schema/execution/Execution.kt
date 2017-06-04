package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode


sealed class Execution {

    open class Node (
            val schemaNode: SchemaNode.Branch,
            val children: Collection<Execution>,
            val key : String,
            val alias: String? = null,
            val arguments : Arguments? = null,
            val typeCondition: TypeCondition? = null,
            val directives: Map<Directive, Arguments?>?
    ) : Execution() {
        val aliasOrKey = alias ?: key
    }

    class Fragment(
            val condition: TypeCondition,
            val elements : List<Execution.Node>,
            val directives: Map<Directive, Arguments?>?
    ) : Execution()

    class Operation<T>(
            val operationNode: SchemaNode.Operation<T>,
            children: Collection<Execution>,
            key : String,
            alias: String? = null,
            arguments : Arguments? = null,
            condition : TypeCondition? = null,
            directives: Map<Directive, Arguments?>?
    ) : Execution.Node(operationNode, children, key, alias, arguments, condition, directives)

    class Union (
            val unionNode : SchemaNode.UnionProperty,
            val memberChildren: Map<SchemaNode.ReturnType, Collection<Execution>>,
            key: String,
            alias: String? = null,
            condition : TypeCondition? = null,
            directives: Map<Directive, Arguments?>?
    ) : Execution.Node (unionNode, emptyList(), key, alias, null, condition, directives) {
        fun memberExecution(type: SchemaNode.ReturnType): Execution.Node {
            return Execution.Node (
                    schemaNode,
                    memberChildren[type] ?: throw IllegalArgumentException("Union ${unionNode.kqlProperty.name} has no member $type"),
                    key,
                    alias,
                    arguments,
                    typeCondition,
                    directives
            )
        }
    }
}