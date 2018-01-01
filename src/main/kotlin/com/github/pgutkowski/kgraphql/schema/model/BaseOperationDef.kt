package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.Context

abstract class BaseOperationDef<T, R>(
        name : String,
        private val operationWrapper: FunctionWrapper<R>,
        val inputValues : List<InputValueDef<*>>,
        val accessRule : ((T?, Context) -> Exception?)?
) : Definition(name), OperationDef<R>, FunctionWrapper<R> by operationWrapper