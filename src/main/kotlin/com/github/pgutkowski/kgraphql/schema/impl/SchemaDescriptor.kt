package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.graph.DescriptorNode
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.GraphBuilder
import com.github.pgutkowski.kgraphql.graph.GraphNode
import com.github.pgutkowski.kgraphql.request.Arguments
import kotlin.reflect.KClass


class SchemaDescriptor internal constructor(
        val schema: DefaultSchema,
        val queries: Graph,
        val mutations: Graph,
        val typeMap: Map<KClass<*>, Graph>
) {

    companion object {
        fun forSchema (schema: DefaultSchema): SchemaDescriptor {
            return SchemaDescriptorBuilder(schema).build()
        }
    }

    fun <T : Any>get(key: KClass<T>) = this.typeMap[key]

    fun intersect(request: Graph) : Graph {
        val graphBuilder = GraphBuilder()
        for(node in request){
            val descriptorNode = queries[node.key] ?: mutations[node.key]
                    ?: throw SyntaxException("${node.key} is not supported by this schema")

            graphBuilder.add(intersectNodes(descriptorNode as DescriptorNode, node))
        }
        return graphBuilder.build()
    }

    private fun intersectNodes(descriptorNode: DescriptorNode, requestNode: GraphNode) : GraphNode {
        var children : Graph? = null

        if(requestNode.children != null && requestNode.children.isNotEmpty()){
            children = intersectChildren(requestNode.children, descriptorNode, requestNode)
        } else if(descriptorNode.children != null){
            children = descriptorNode.children
        }

        var arguments : Arguments? = null

        if(requestNode.arguments != null){
            arguments = intersectArguments(descriptorNode, requestNode.arguments, requestNode.aliasOrKey)
        }

        return GraphNode(requestNode.key, requestNode.alias, children, arguments)
    }

    private fun intersectChildren(children: Graph, descriptorNode: DescriptorNode, requestNode: GraphNode): Graph {
        val childrenNodes = mutableListOf<GraphNode>()
        for (requestNodeChild in children) {
            val descriptorNodeChild = descriptorNode.children?.find { it.key == requestNodeChild.key } as DescriptorNode?
            if (descriptorNodeChild != null) {
                childrenNodes.add(intersectNodes(descriptorNodeChild, requestNodeChild))
            } else {
                throw IllegalArgumentException("${requestNode.key} doesn't have property: ${requestNodeChild.key}")
            }
        }
        return Graph(*childrenNodes.toTypedArray())
    }

    private fun intersectArguments(descriptorNode: DescriptorNode, requestArguments : Arguments, aliasOrKey: String) : Arguments? {
        if(requestArguments.size != descriptorNode.argumentsDescriptor?.size){
            throw SyntaxException("$aliasOrKey does support arguments: ${descriptorNode.argumentsDescriptor?.keys}. " +
                    "found arguments: ${requestArguments.keys}")
        }

        val arguments = Arguments()
        for((key, value) in requestArguments){
            if(descriptorNode.argumentsDescriptor[key] != null){
                arguments.put(key, value)
            } else {
                throw IllegalArgumentException("Invalid argument: $key on field: $aliasOrKey")
            }
        }

        return arguments
    }
}