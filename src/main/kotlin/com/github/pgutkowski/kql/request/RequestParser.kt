package com.github.pgutkowski.kql.request


class RequestParser(private val actionResolver: (String) -> ParsedRequest.Action) {

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
            else -> throw IllegalArgumentException("Invalid request header: ${getRequestHeader(request)}")
        }

        val requestBody = dropRequestHeader(request)

        if(action == null){
            action = actionResolver.invoke(requestBody.trim().split(" ", ",").first())
        }

        //TODO
        return ParsedRequest(action, MultiMap(), name)
    }

    fun dropRequestHeader(request : String) = request.substringAfter("{", "").substringBeforeLast("}", "").trim()

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