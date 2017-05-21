package com.github.pgutkowski.kgraphql.graph

/**
 * fragment key needs to start with "..."
 */
class Fragment(key : String,
               val fragmentGraph: Graph,
               val typeCondition: String? = null
) : GraphNode(key, null, fragmentGraph, null)