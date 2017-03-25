package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.query.Result
import com.github.pgutkowski.kql.schema.Schema

/**
 * types are stored as map of name -> KQLType, to speed up lookup time. Data duplication is bad, but this is necessary
 */
class DefaultSchema(val types: HashMap<String, MutableList<KQLType<*>>>) : Schema {
    override fun handleQuery(query: String): Result {
        return Result(null, null)
    }
}