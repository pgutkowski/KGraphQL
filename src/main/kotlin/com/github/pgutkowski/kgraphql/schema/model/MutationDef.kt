package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.Context

class MutationDef<R> (
        name : String,
        resolver: FunctionWrapper<R>,
        override val description : String?,
        override val isDeprecated: Boolean,
        override val deprecationReason: String?,
        accessRule: ((Nothing?, Context) -> Exception?)? = null,
        inputValues : List<InputValueDef<*>> = emptyList()
) : BaseOperationDef<Nothing, R>(name, resolver, inputValues, accessRule), DescribedDef
