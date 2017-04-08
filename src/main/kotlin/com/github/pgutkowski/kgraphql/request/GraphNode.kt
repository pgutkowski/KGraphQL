package com.github.pgutkowski.kgraphql.request

//simulating union type
sealed class GraphNode(val key: String) {

    companion object {
        fun of(key: String, referred : Any?) : GraphNode {
            return when (referred) {
                null -> Leaf(key)
                is Graph -> ToGraph(key, referred)
                is Arguments -> ToArguments(key, referred)
                else -> throw IllegalArgumentException(
                        "GraphNode cannot refer of instance of ${referred.javaClass}. "
                        + "Supported are only null, ${Graph::class} and ${Arguments::class}"
                )
            }
        }
    }

    class Leaf(key: String) : GraphNode(key) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Leaf

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }
    }

    class ToGraph(key: String, val graph: Graph) : GraphNode(key){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as ToGraph

            if (key != other.key) return false
            if (graph != other.graph) return false

            return true
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + graph.hashCode()
            return result
        }
    }

    class ToArguments(key: String, val arguments : Arguments, val graph: Graph? = null) : GraphNode(key){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as ToArguments

            if (key != other.key) return false
            if (arguments != other.arguments) return false
            if (graph != other.graph) return false

            return true
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + arguments.hashCode()
            if(graph != null) result = 31 * result + graph.hashCode()
            return result
        }
    }
}