package com.github.pgutkowski.kgraphql.request


open class Graph() : ArrayList<GraphNode>(){

    companion object {
        fun leaf(key : String) = GraphNode.Leaf(key)

        fun leafs(vararg keys : String): Array<GraphNode.Leaf> {
            return keys.map { GraphNode.Leaf(it) }.toTypedArray()
        }

        fun branch(key: String, graph: Graph) = GraphNode.ToGraph(key, graph)

        fun branch(key: String, vararg nodes: GraphNode) = GraphNode.ToGraph(key, Graph(*nodes))

        fun argLeaf(key: String, args:  Arguments) = GraphNode.ToArguments(key, args)

        fun args(vararg args: Pair<String, String>) = Arguments(*args)

        fun argBranch(key: String, args: Arguments, vararg nodes: GraphNode): GraphNode.ToArguments {
            return GraphNode.ToArguments(key, args, if(nodes.isNotEmpty()) Graph(*nodes) else null)
        }

        fun argBranch(key: String, args: Arguments, graph : Graph?): GraphNode.ToArguments {
            return GraphNode.ToArguments(key, args, graph)
        }
    }

    constructor(vararg nodes: GraphNode) : this(){
        this.addAll(nodes)
    }
}