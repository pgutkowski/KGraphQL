package com.github.pgutkowski.kgraphql.schema.structure2

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.request.Operation
import com.github.pgutkowski.kgraphql.request.OperationVariable
import com.github.pgutkowski.kgraphql.request.graph.DirectiveInvocation
import com.github.pgutkowski.kgraphql.request.graph.Fragment
import com.github.pgutkowski.kgraphql.request.graph.SelectionNode
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.execution.Execution
import com.github.pgutkowski.kgraphql.schema.execution.ExecutionPlan
import com.github.pgutkowski.kgraphql.schema.execution.TypeCondition


class RequestInterpreter(val schemaModel: SchemaModel) {

    private val directivesByName = schemaModel.directives.associate { it.name to it }

    fun createExecutionPlan(request: Operation) : ExecutionPlan {
        val root = getRoot(request)

        val operations = request.selectionTree.map { root.handleSelection(it, request.variables) }

        return ExecutionPlan(operations)
    }

    private fun getRoot(request: Operation): Type {
        return when (request.action) {
            Operation.Action.QUERY -> schemaModel.query
            Operation.Action.MUTATION -> schemaModel.mutation
            else -> {
                val keys = request.selectionTree.nodes.map { it.key }
                when {
                    keys.all { schemaModel.query.hasField(it) } -> schemaModel.query
                    keys.all { schemaModel.mutation.hasField(it) } -> schemaModel.mutation
                    else -> {
                        handleUnsupportedOperations(keys)
                        throw RequestException("Cannot infer operation from fields")
                    }
                }
            }
        }
    }

    private fun handleChildren(field: Field, requestNode: SelectionNode): List<Execution> {
        return handleReturnType(field.returnType, requestNode)
    }

    private fun handleReturnType(type: Type, requestNode: SelectionNode): List<Execution> {
        val children = mutableListOf<Execution>()

        if (requestNode.children != null) {
            requestNode.children.mapTo(children) { handleReturnTypeChildOrFragment(it, type) }
        } else if(type.unwrapped().fields?.isNotEmpty() ?: false){
            throw RequestException("Missing selection set on property ${requestNode.key} of type ${type.unwrapped().name}")
        }

        return children
    }

    private fun handleReturnTypeChildOrFragment(node: SelectionNode, returnType: Type): Execution {
        val unwrappedType = returnType.unwrapped()

        return when(node){
            is Fragment -> {
                val conditionType = findFragmentType(node, unwrappedType)
                val condition = TypeCondition(conditionType)
                val elements = node.fragmentGraph.map { conditionType.handleSelection(it) }
                Execution.Fragment(condition, elements, node.directives?.lookup())
            }
            else -> {
                unwrappedType.handleSelection(node)
            }
        }
    }

    private fun findFragmentType(fragment: Fragment, enclosingType: Type) : Type {
        when(fragment){
            is Fragment.External -> {
                return schemaModel.queryTypesByName[fragment.typeCondition] ?: throw throwUnknownFragmentTypeEx(fragment)
            }
            is Fragment.Inline -> {
                if(fragment.typeCondition == null && fragment.directives?.isNotEmpty() ?: false){
                    return enclosingType
                } else {
                    return schemaModel.queryTypesByName[fragment.typeCondition] ?: throw throwUnknownFragmentTypeEx(fragment)
                }
            }
            else -> throw ExecutionException("Unexpected fragment type: ${fragment.javaClass}")
        }
    }

    private fun Type.handleSelection(selectionNode: SelectionNode, variables: List<OperationVariable>? = null): Execution.Node {
        val field = this[selectionNode.key]

        return when(field){
            null -> throw RequestException("property ${selectionNode.key} on ${this.name} does not exist")
            is Field.Union<*> -> handleUnion(field, selectionNode)
            else -> {
                validatePropertyArguments(this, field, selectionNode)

                return Execution.Node(
                        field = field,
                        children = handleChildren(field, selectionNode),
                        key = selectionNode.key,
                        alias = selectionNode.alias,
                        arguments = selectionNode.arguments,
                        directives = selectionNode.directives?.lookup(),
                        variables = variables
                )

            }
        }
    }

    private fun <T> handleUnion(field: Field.Union<T>, selectionNode: SelectionNode) : Execution.Union {
        validateUnionRequest(field, selectionNode)

        val unionMembersChildren = field.returnType.possibleTypes.associate { possibleType ->
            val fragmentRequestNode = selectionNode.children?.get("on ${possibleType.name}")
                    ?: selectionNode.children?.filterIsInstance<Fragment.External>()?.find {possibleType.name == it.typeCondition }
                    ?: throw RequestException("Missing type argument for type ${possibleType.name}")

            possibleType to handleReturnType(possibleType, fragmentRequestNode)
        }
        return Execution.Union (
                unionField = field,
                memberChildren = unionMembersChildren,
                key = selectionNode.key,
                alias = selectionNode.alias,
                condition = null,
                directives = selectionNode.directives?.lookup()
        )
    }


    private fun throwUnknownFragmentTypeEx(fragment: Fragment) : RequestException {
        return RequestException("Unknown type ${fragment.typeCondition} in type condition on fragment ${fragment.fragmentKey}")
    }


    private fun handleUnsupportedOperations(keys: List<String>) {
        keys.forEach { key ->
            if (!schemaModel.query.hasField(key) && !schemaModel.mutation.hasField(key)) {
                throw RequestException("$key is not supported by this schema")
            }
        }
    }

    fun List<DirectiveInvocation>.lookup() = associate { findDirective(it) to it.arguments }

    fun findDirective(invocation : DirectiveInvocation) : Directive {
        return directivesByName[invocation.key.removePrefix("@")]
                ?: throw RequestException("Directive ${invocation.key} does not exist")
    }
}