package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.request.graph.Fragment
import java.util.*


/**
 * Represents half-structured query document data.
 * Document content is split on lists of [FragmentTokens] and [OperationTokens]
 */
data class Document(val fragmentsTokens: List<FragmentTokens>, val operationTokens: List<OperationTokens>){

    /**
     * Represents half-structured data of fragment declaration in query document.
     */
    data class FragmentTokens(val name : String, val typeCondition: String, val graphTokens : List<String>)

    /**
     * Represents half-structured data of operation declaration in query document
     */
    data class OperationTokens(val name : String?, val type: String?, val variables: List<OperationVariable>?, val graphTokens :     List<String>)

    class Fragments (
            tokensList: List<FragmentTokens>,
            private val transformer: (Fragments, FragmentTokens) -> Fragment.External
    ) {
        private val tokensMap = tokensList.associate { "...${it.name}" to it }

        private val transformed = mutableMapOf<String, Fragment.External>()

        //prevent stack overflow
        private val fragmentsStack = Stack<String>()

        operator fun get(name : String): Fragment.External {
            if(fragmentsStack.contains(name)) throw RequestException("Fragment spread circular references are not allowed")

            return transformed.getOrPut(name) {
                fragmentsStack.push(name)
                val fragment = transformer(this, tokensMap[name] ?: throw IllegalArgumentException("Fragment $name does not exist"))
                fragmentsStack.pop()
                fragment
            }
        }
    }
}