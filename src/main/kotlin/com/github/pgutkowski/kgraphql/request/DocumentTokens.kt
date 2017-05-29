package com.github.pgutkowski.kgraphql.request


data class DocumentTokens(val fragmentsTokens: List<FragmentTokens>, val operationTokens: List<OperationTokens>){

    data class FragmentTokens(val name : String, val typeCondition: String?, val graphTokens : List<String>)

    data class OperationTokens(val name : String?, val type: String?, val graphTokens : List<String>)
}