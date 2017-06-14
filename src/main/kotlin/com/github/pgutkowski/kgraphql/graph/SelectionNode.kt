package com.github.pgutkowski.kgraphql.graph

import com.github.pgutkowski.kgraphql.request.Arguments

open class SelectionNode(
        val key : String,
        val alias: String? = null,
        val children: SelectionTree? = null,
        val arguments : Arguments? = null,
        val directives: List<DirectiveInvocation>? = null
) : Comparable<SelectionNode> {

    override fun compareTo(other: SelectionNode): Int {
        return this.aliasOrKey.compareTo(other.aliasOrKey)
    }

    val aliasOrKey = alias ?: key

    val isLeaf: Boolean
        get() = children == null || children.isEmpty()

    val isBranch: Boolean
        get() = children != null && children.isNotEmpty()

    val hasArguments: Boolean
        get() = arguments != null

    override fun toString(): String {
        val builder = StringBuilder(aliasOrKey)
        if(children != null) builder.append(" {").append(children).append("} ")
        if(arguments != null) builder.append(" args: ").append("{").append(arguments).append("}")
        if(directives != null) builder.append(" directives: ").append("{").append(arguments).append("}")
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SelectionNode

        if (key != other.key) return false
        if (alias != other.alias) return false
        if (children != other.children) return false
        if (arguments != other.arguments) return false
        if (directives != other.directives) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (alias?.hashCode() ?: 0)
        result = 31 * result + (children?.hashCode() ?: 0)
        result = 31 * result + (arguments?.hashCode() ?: 0)
        result = 31 * result + (directives?.hashCode() ?: 0)
        return result
    }
}