package com.github.pgutkowski.kgraphql.graph

import com.github.pgutkowski.kgraphql.request.Arguments

open class GraphNode(
        val key : String,
        val alias: String? = null,
        val children: Graph? = null,
        val arguments : Arguments? = null
) {
    val aliasOrKey = alias ?: key

    val isLeaf: Boolean
        get() = children == null || children.isEmpty()

    val isBranch: Boolean
        get() = children != null && children.isNotEmpty()

    val hasArguments: Boolean
        get() = arguments != null

    override fun toString(): String {
        val builder = StringBuilder(aliasOrKey)
        if(children != null) builder.append(" ").append(children)
        if(arguments != null) builder.append(" args: ").append(arguments)
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as GraphNode

        if (key != other.key) return false
        if (alias != other.alias) return false
        if (children != other.children) return false
        if (arguments != other.arguments) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (alias?.hashCode() ?: 0)
        result = 31 * result + (children?.hashCode() ?: 0)
        result = 31 * result + (arguments?.hashCode() ?: 0)
        return result
    }
}