package com.github.pgutkowski.kgraphql.graph

/**
 * fragment key needs to start with "..."
 */
interface Fragment {

    val fragmentGraph: Graph

    /**
     * If the typeCondition is omitted, an inline fragment is considered to be of the same type as the enclosing context
     */
    val typeCondition: String?

    class External (
            key : String,
            override val fragmentGraph: Graph,
            override val typeCondition: String
    ) : Fragment, GraphNode(key, null, fragmentGraph, null){
        init {
            if(!key.startsWith("...")){
                throw IllegalArgumentException("External fragment key has to start with '...'")
            }
        }
    }

    class Inline (
            override val fragmentGraph: Graph,
            override val typeCondition: String?
    ) : Fragment, GraphNode("on $typeCondition", null, fragmentGraph, null)
}