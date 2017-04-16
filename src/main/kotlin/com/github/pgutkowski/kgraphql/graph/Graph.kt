package com.github.pgutkowski.kgraphql.graph

import com.github.pgutkowski.kgraphql.request.Arguments


open class Graph() : ArrayList<GraphNode>() {

    constructor(vararg graphNodes: GraphNode) : this() {
        addAll(graphNodes)
    }

    companion object {

        fun leaf(key : String, alias: String? = null) = GraphNode(key, alias)

        fun leafs(vararg keys : String): Array<GraphNode> {
            return keys.map { GraphNode(it) }.toTypedArray()
        }

        fun branch(key: String, vararg nodes: GraphNode) = GraphNode(key = key, alias = null, children = Graph(*nodes))

        fun argLeaf(key: String, args: Arguments) = GraphNode(key = key, alias = null, children = null, arguments = args)

        fun args(vararg args: Pair<String, String>) = Arguments(*args)

        fun argBranch(key: String, args: Arguments, vararg nodes: GraphNode): GraphNode {
            return GraphNode(key = key, alias = null, children = if (nodes.isNotEmpty()) Graph(*nodes) else null, arguments = args)
        }

        fun argBranch(key: String, alias: String, args: Arguments, vararg nodes: GraphNode): GraphNode {
            return GraphNode(key, alias, if (nodes.isNotEmpty()) Graph(*nodes) else null, args)
        }
    }

    operator fun get(aliasOrKey: String) = find { it.aliasOrKey == aliasOrKey }
}