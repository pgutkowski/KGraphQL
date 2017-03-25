package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.request.ParsedRequest
import com.github.pgutkowski.kql.request.RequestParser
import com.github.pgutkowski.kql.request.result.Result
import com.github.pgutkowski.kql.schema.Schema

/**
 * types are stored as map of name -> KQLType, to speed up lookup time. Data duplication is bad, but this is necessary
 */
class DefaultSchema(
        val types: HashMap<String, MutableList<KQLObject>>,
        val queries: ArrayList<KQLObject.Query<*>>,
        val mutations: ArrayList<KQLObject.Mutation>,
        val inputs: ArrayList<KQLObject.Input<*>>,
        val scalars: ArrayList<KQLObject.Scalar<*, *>>
) : Schema {

    private val requestParser = RequestParser { resolveActionType(it) }

    override fun handleRequest(request: String): Result {
        return Result(null, null)
    }

    fun resolveActionType(token : String) : ParsedRequest.Action{
        if (queries.any { it.name == token }) return ParsedRequest.Action.QUERY
        if (mutations.flatMap { it.functions }.any { it.name == token }) return ParsedRequest.Action.MUTATION
        throw IllegalArgumentException("Cannot infer request type for name $token")
    }
}