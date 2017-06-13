package com.github.pgutkowski.kgraphql.request

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
    data class OperationTokens(val name : String?, val type: String?, val variables: List<OperationVariable>?, val graphTokens : List<String>)
}