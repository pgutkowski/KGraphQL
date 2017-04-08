package com.github.pgutkowski.kql.request

import com.github.pgutkowski.kql.SyntaxException


class RequestParser(private val actionResolver: (String) -> Request.Action) {

    val graphParser: GraphParser = GraphParser()

    fun parse(request: String) : Request {
        /**
         * first see if request is named and easily categorized as query or mutations
         */
        val requestHeaderTokens = getRequestHeaderTokens(request)
        var name : String? = null
        var action : Request.Action? = null

        when(requestHeaderTokens.size){
            0 -> name = ""
            1 ->{
                name = ""
                action = Request.Action.valueOf(requestHeaderTokens.first().toUpperCase())
            }
            2 -> {
                action = Request.Action.valueOf(requestHeaderTokens.first().toUpperCase())
                name = requestHeaderTokens[1]
            }
            //not supporting arguments yet
            else -> throw SyntaxException("Invalid request header: ${getRequestHeader(request)}")
        }

        val requestBody = dropRequestHeader(request)

        /*if(action == null){
            action = actionResolver.invoke(requestBody.trim().split(" ", ",").first())
        }*/

        val body = graphParser.parse(requestBody)

        val actions = body.map { it.key }
        if(actions.isEmpty()) throw SyntaxException("Invalid query: $requestBody, no fields specified")

        if(actions.size == 1){
            if(action == null){
                action = actionResolver((actions.first()))
            }
        } else {
            action = actionResolver(actions.first())
            //ensure that all fields represent same action
            if(!actions.all { actionResolver(it) == action }){
                throw SyntaxException("Cannot execute mutations with queries in single request")
            }
        }

        //TODO
        return Request(action, body, name)
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