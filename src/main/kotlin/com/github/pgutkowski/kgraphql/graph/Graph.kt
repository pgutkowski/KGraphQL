package com.github.pgutkowski.kgraphql.graph

import com.github.pgutkowski.kgraphql.request.Arguments
import java.util.*


class GraphBuilder : ArrayList<GraphNode>(){

    fun build() : Graph {
        return Graph(*this.toTypedArray())
    }
}

class Graph(vararg graphNodes: GraphNode) : Collection<GraphNode> {

    init {
        Arrays.sort(graphNodes)
    }

    val nodes = graphNodes

    /*collection interface*/
    override fun contains(element: GraphNode) = nodes.contains(element)

    override fun containsAll(elements: Collection<GraphNode>) : Boolean {
        return elements.map { nodes.contains(it) }.reduce { acc, contains -> acc && contains }
    }

    override fun isEmpty() = nodes.isEmpty()

    override val size: Int = nodes.size

    override fun iterator(): Iterator<GraphNode> = nodes.iterator()

    /*custom graph logic*/
    operator fun get(aliasOrKey: String) = find { it.aliasOrKey == aliasOrKey }

    /*Any interface*/
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Graph

        //other does not matter for graph
        if (!Arrays.equals(this.nodes, other.nodes)) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(nodes)
        result = 31 * result + size
        return result
    }

    override fun toString(): String {
        return "Graph(${Arrays.toString(nodes)})"
    }
}

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