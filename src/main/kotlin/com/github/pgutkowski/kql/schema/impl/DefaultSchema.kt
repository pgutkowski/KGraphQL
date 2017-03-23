package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.query.Result
import com.github.pgutkowski.kql.schema.Schema
import com.github.pgutkowski.kql.support.ClassSupport
import kotlin.reflect.KClass


class DefaultSchema(classes: HashMap<KClass<*>, Array<out ClassSupport<*>>>) : Schema {
    override fun handleQuery(query: String): Result {
        return Result(null, null)
    }
}