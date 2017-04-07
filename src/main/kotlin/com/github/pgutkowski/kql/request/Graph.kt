package com.github.pgutkowski.kql.request


open class Graph() : ArrayList<GraphNode>(){
    constructor(vararg nodes: GraphNode) : this(){
        this.addAll(nodes)
    }
}