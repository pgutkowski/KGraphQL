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

    val fragmentKey : String

    class External (
            override val fragmentKey : String,
            override val fragmentGraph: Graph,
            override val typeCondition: String
    ) : Fragment, GraphNode(fragmentKey, null, fragmentGraph, null){
        init {
            if(!key.startsWith("...")){
                throw IllegalArgumentException("External fragment key has to start with '...'")
            }
        }
    }

    class Inline (
            override val fragmentGraph: Graph,
            override val typeCondition: String?,
            directives: List<DirectiveInvocation>?,
            @Deprecated("It is highly not recommended to override default value of fragmentKey for inline fragments")
            override val fragmentKey: String = "on $typeCondition"
    ) : Fragment, GraphNode(fragmentKey, null, fragmentGraph, null, directives)
}