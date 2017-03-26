package com.github.pgutkowski.kql.request

import com.github.pgutkowski.kql.request.serialization.DefaultGraphDeserializer
import com.github.pgutkowski.kql.request.serialization.GraphDeserializer


class RequestParser(private val actionResolver: (String) -> ParsedRequest.Action) {

    val graphDeserializer: GraphDeserializer = DefaultGraphDeserializer()

    fun parse(request: String) : ParsedRequest {
        /**
         * first see if request is named and easily categorized as query or mutations
         */
        val requestHeaderTokens = getRequestHeaderTokens(request)
        var name : String? = null
        var action : ParsedRequest.Action? = null

        when(requestHeaderTokens.size){
            0 -> name = ""
            1 ->{
                name = ""
                action = ParsedRequest.Action.valueOf(requestHeaderTokens.first().toUpperCase())
            }
            2 -> {
                action = ParsedRequest.Action.valueOf(requestHeaderTokens.first().toUpperCase())
                name = requestHeaderTokens[1]
            }
            //not supporting arguments yet
            else -> throw SyntaxException("Invalid request header: ${getRequestHeader(request)}")
        }

        val requestBody = dropRequestHeader(request)

        /*if(action == null){
            action = actionResolver.invoke(requestBody.trim().split(" ", ",").first())
        }*/

        val body = graphDeserializer.deserialize(requestBody)

        val actions = body.keys
        if(actions.isEmpty()) throw SyntaxException("Invalid query: $requestBody, no fields specified")

        if(actions.size == 1){
            if(action == null){
                action = actionResolver.invoke(actions.first())
            }
        } else {
            action = actionResolver.invoke(actions.first())
            //ensure that all fields represent same action
            if(!actions.all { actionResolver.invoke(it) == action }){
                throw SyntaxException("Cannot execute mutations with queries in single request")
            }
        }

        //TODO
        return ParsedRequest(action, body, name)
    }

    fun dropRequestHeader(request : String) = request.substring(request.indexOf('{'), request.lastIndexOf('}')+1).trim()

    private fun getRequestHeader(request : String) = request.substringBefore("{", "").trim()

    fun getRequestHeaderTokens(request: String): List<String> {
        val header = getRequestHeader(request)

        if(header.isBlank()){
            return emptyList()
        } else{
            return header.split(" ")
        }
    }
}