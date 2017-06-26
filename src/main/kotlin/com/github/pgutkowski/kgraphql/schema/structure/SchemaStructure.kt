package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.request.Operation
import com.github.pgutkowski.kgraphql.request.OperationVariable
import com.github.pgutkowski.kgraphql.request.graph.DirectiveInvocation
import com.github.pgutkowski.kgraphql.request.graph.Fragment
import com.github.pgutkowski.kgraphql.request.graph.SelectionNode
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.execution.Execution
import com.github.pgutkowski.kgraphql.schema.execution.ExecutionPlan
import com.github.pgutkowski.kgraphql.schema.execution.TypeCondition
import com.github.pgutkowski.kgraphql.schema.model.SchemaDefinition
import kotlin.reflect.KType


class SchemaStructure (
        val queries : Map<String, SchemaNode.Query<*>>,
        val mutations : Map<String, SchemaNode.Mutation<*>>,
        val queryTypes: Map<KType, SchemaNode.Type>,
        val inputTypes: Map<KType, SchemaNode.Type>,
        val directives: Map<String, Directive>
) {

    companion object {
        fun of(schema: SchemaDefinition) : SchemaStructure = SchemaStructureBuilder(schema).build()
    }

    val queryTypeByName = queryTypes.values.associate { it.kqlType.name to it }

    val inputTypeByName = inputTypes.values.associate { it.kqlType.name to it }

    fun createExecutionPlan(request: Operation) : ExecutionPlan {
        val children = mutableListOf<Execution.Operation<*>>()
        val root = getRoot(request)

        for(requestNode in request.selectionTree){
            val operation = root[requestNode.key]
                    ?: throw RequestException("${requestNode.key} is not supported by this schema")
            children.add(handleOperation(requestNode, operation, request.variables))
        }

        return ExecutionPlan(children)
    }

    private fun getRoot(request: Operation): Map<String, SchemaNode.Operation<*>> {
        return when (request.action) {
            Operation.Action.QUERY -> queries
            Operation.Action.MUTATION -> mutations
            else -> {
                val keys = request.selectionTree.nodes.map { it.key }
                when {
                    keys.all { queries.containsKey(it) } -> queries
                    keys.all { mutations.containsKey(it) } -> mutations
                    else -> {
                        handleUnsupportedOperations(keys)
                        throw RequestException("Cannot infer operation from fields")
                    }
                }
            }
        }
    }

    private fun handleUnsupportedOperations(keys: List<String>) {
        keys.forEach { key ->
            if (queries.none { it.key == key } && mutations.none { it.key == key }) {
                throw RequestException("$key is not supported by this schema")
            }
        }
    }

    private fun <T>handleOperation(requestNode: SelectionNode,
                                   operation: SchemaNode.Operation<T>,
                                   variables: List<OperationVariable>?): Execution.Operation<T>{
        return Execution.Operation(
                operationNode = operation,
                children = handleChildren(operation, requestNode),
                key = requestNode.key,
                alias = requestNode.alias,
                arguments = requestNode.arguments,
                directives = requestNode.directives?.lookup(),
                variables = variables
        )
    }

    private fun handleBranch(requestNode: SelectionNode, operation: SchemaNode.SingleBranch): Execution.Node {
        return Execution.Node(
                schemaNode = operation,
                children = handleChildren(operation, requestNode),
                key = requestNode.key,
                alias = requestNode.alias,
                arguments = requestNode.arguments,
                directives = requestNode.directives?.lookup()
        )
    }

    fun List<DirectiveInvocation>.lookup() = associate { findDirective(it) to it.arguments }

    private fun handleUnion(requestNode: SelectionNode, property: SchemaNode.UnionProperty): Execution.Union {
        validateUnionRequest(requestNode, property)

        val unionMembersChildren = property.returnTypes.associate { returnType ->
            val fragmentRequestNode = requestNode.children?.get("on ${returnType.kqlType.name}")
                    ?: requestNode.children?.filterIsInstance<Fragment.External>()?.find {returnType.kqlType.name == it.typeCondition }
                    ?: throw RequestException("Missing type argument for type ${returnType.kqlType.name}")

            returnType to handleReturnType(returnType, fragmentRequestNode)
        }
        return Execution.Union (
                unionNode = property,
                memberChildren = unionMembersChildren,
                key = requestNode.key,
                alias = requestNode.alias,
                condition = null,
                directives = requestNode.directives?.lookup()
        )
    }

    private fun handleChildren(operation: SchemaNode.SingleBranch, requestNode: SelectionNode): List<Execution> {
        return handleReturnType(operation.returnType, requestNode)
    }

    fun handleReturnType(returnType: SchemaNode.ReturnType, requestNode: SelectionNode) : List<Execution>{
        val children = mutableListOf<Execution>()
        if (requestNode.children != null) {
            val childrenRequestNodes = requestNode.children
            childrenRequestNodes.mapTo(children) { handleReturnTypeChildOrFragment(it, returnType) }
        } else if(returnType.properties.isNotEmpty()){
            throw RequestException("Missing selection set on property ${requestNode.aliasOrKey} of type ${returnType.kqlType.name}")
        }
        return children
    }

    private fun handleReturnTypeChildOrFragment(node: SelectionNode, returnType: SchemaNode.ReturnType): Execution {
        return when(node){
            is Fragment -> {
                val type = findFragmentType(node, returnType)
                val condition = TypeCondition(type)
                val elements = node.fragmentGraph.map { handleTypeChild(it, type) }
                Execution.Fragment(condition, elements, node.directives?.lookup())
            }
            else -> {
                handleTypeChild(node, returnType)
            }
        }
    }

    private fun findFragmentType(fragment: Fragment, enclosingType: SchemaNode.ReturnType) : SchemaNode.Type {
        when(fragment){
            is Fragment.External -> {
                return findNodeByTypeName(fragment.typeCondition) ?: throw throwUnknownFragmentTypeEx(fragment)
            }
            is Fragment.Inline -> {
                if(fragment.typeCondition == null && fragment.directives?.isNotEmpty() ?: false){
                    return enclosingType.type
                } else {
                    return findNodeByTypeName(fragment.typeCondition) ?: throw throwUnknownFragmentTypeEx(fragment)
                }
            }
            else -> throw ExecutionException("Unexpected fragment type: ${fragment.javaClass}")
        }
    }

    private fun throwUnknownFragmentTypeEx(fragment: Fragment) : RequestException {
        return RequestException("Unknown type ${fragment.typeCondition} in type condition on fragment ${fragment.fragmentKey}")
    }

    private fun handleTypeChild(selectionNode: SelectionNode, returnType: SchemaNode.Type): Execution.Node {
        val property = returnType.properties[selectionNode.key]
        val unionProperty = returnType.unionProperties[selectionNode.key]

        when {
            property == null && unionProperty == null -> {
                throw RequestException("property ${selectionNode.key} on ${returnType.kqlType.name} does not exist")
            }
            property != null && unionProperty == null -> {
                validatePropertyArguments(property, returnType.kqlType, selectionNode)
                return handleBranch(selectionNode, property)
            }
            property == null && unionProperty != null -> {
                return handleUnion(selectionNode, unionProperty)
            }
            else -> throw SchemaException("Invalid schema structure - type contains doubling properties")
        }
    }

    fun findDirective(invocation : DirectiveInvocation) : Directive {
        return directives[invocation.key.removePrefix("@")] ?: throw RequestException("Directive ${invocation.key} does not exist")
    }

    fun findNodeByTypeName(typeName: String?) : SchemaNode.Type? {
        return queryTypes.values.find { it.kqlType.name == typeName }
    }
}