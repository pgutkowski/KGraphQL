package com.github.pgutkowski.kql.schema.impl.function

import com.github.pgutkowski.kql.resolve.QueryResolver
import kotlin.reflect.KFunction

class QueryFunction(resolver: QueryResolver<*>, function: KFunction<*>) : FunctionWrapper<QueryResolver<*>>(resolver, function)