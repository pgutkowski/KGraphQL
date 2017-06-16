package com.github.pgutkowski.kgraphql.request.graph

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.request.Arguments
import java.util.*


class SelectionSetBuilder : ArrayList<SelectionNode>(){
    fun build() : SelectionTree {
        return SelectionTree(*this.toTypedArray())
    }

    override fun add(element: SelectionNode): Boolean {
        when {
            element.key.isBlank() -> throw SyntaxException("cannot handle blank property in object : $this")
            any { it.aliasOrKey == element.aliasOrKey } -> return false
            else -> return super.add(element)
        }
    }
}

data class SelectionTree(val nodes: List<SelectionNode>) : Collection<SelectionNode> by nodes {

    constructor(vararg graphNodes : SelectionNode) : this(graphNodes.toList())

    operator fun get(aliasOrKey: String) = find { it.aliasOrKey == aliasOrKey }
}

fun leaf(key : String, alias: String? = null) = SelectionNode(key, alias)

fun leafs(vararg keys : String): Array<SelectionNode> {
    return keys.map { SelectionNode(it) }.toTypedArray()
}

fun branch(key: String, vararg nodes: SelectionNode) = SelectionNode(key = key, alias = null, children = SelectionTree(*nodes))

fun argLeaf(key: String, args: Arguments) = SelectionNode(key = key, alias = null, children = null, arguments = args)

fun args(vararg args: Pair<String, String>) = Arguments(*args)

fun argBranch(key: String, args: Arguments, vararg nodes: SelectionNode): SelectionNode {
    return SelectionNode(key = key, alias = null, children = if (nodes.isNotEmpty()) SelectionTree(*nodes) else null, arguments = args)
}

fun argBranch(key: String, alias: String, args: Arguments, vararg nodes: SelectionNode): SelectionNode {
    return SelectionNode(key, alias, if (nodes.isNotEmpty()) SelectionTree(*nodes) else null, args)
}

fun extFragment(key: String, typeCondition: String, vararg nodes: SelectionNode) : Fragment.External {
    return Fragment.External(key, SelectionTree(*nodes), typeCondition)
}

fun inlineFragment(typeCondition: String, vararg nodes: SelectionNode) : Fragment.Inline {
    return Fragment.Inline(SelectionTree(*nodes), typeCondition, null)
}