package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.query.Result
import com.github.pgutkowski.kql.schema.Schema

//TODO: waiting for better idea for constructor when query handling will be prototyped
class DefaultSchema(private val queries: ArrayList<KQLType.Query<*>>,
                    private val inputs: ArrayList<KQLType.Input<*>>,
                    private val mutations: ArrayList<KQLType.Mutation<*>>,
                    private val scalars: ArrayList<KQLType.Scalar<*>>,
                    private val simple: ArrayList<KQLType.Simple<*>>,
                    private val interfaces: ArrayList<KQLType.Interface<*>>
) : Schema {

    override fun handleQuery(query: String): Result {
        return Result(null, null)
    }
}