package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.Context

class QueryDef<R> (
        name : String,
        resolver: FunctionWrapper<R>,
        override val description : String? = null,
        override val isDeprecated: Boolean = false,
        override val deprecationReason: String? = null,
        accessRule: ((Nothing?, Context) -> Exception?)? = null,
        inputValues : List<InputValueDef<*>> = emptyList()
) : BaseOperationDef<Nothing, R>(name, resolver, inputValues, accessRule), DescribedDef