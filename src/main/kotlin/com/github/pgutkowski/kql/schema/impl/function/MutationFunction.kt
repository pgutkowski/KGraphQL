package com.github.pgutkowski.kql.schema.impl.function

import com.github.pgutkowski.kql.resolve.MutationResolver
import kotlin.reflect.KFunction

class MutationFunction(resolver: MutationResolver, function: KFunction<*>) : FunctionWrapper<MutationResolver>(resolver, function)