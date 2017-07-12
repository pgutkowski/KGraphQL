package com.github.pgutkowski.kgraphql.schema.model

class QueryDef<R> (
        name : String,
        resolver: FunctionWrapper<R>,
        override val description : String? = null,
        override val isDeprecated: Boolean = false,
        override val deprecationReason: String? = null,
        inputValues : List<InputValueDef<*>> = emptyList()
) : BaseOperationDef<R>(name, resolver, inputValues), DescribedDef