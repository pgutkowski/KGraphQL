package com.github.pgutkowski.kgraphql.schema.model

abstract class BaseOperationDef<T>(
        name : String,
        private val operationWrapper: FunctionWrapper<T>,
        val inputValues : List<InputValueDef<*>>
) : Definition(name), OperationDef<T>, FunctionWrapper<T> by operationWrapper