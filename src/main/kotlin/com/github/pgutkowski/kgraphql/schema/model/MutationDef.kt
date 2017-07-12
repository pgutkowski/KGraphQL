package com.github.pgutkowski.kgraphql.schema.model

class MutationDef<R> (
        name : String,
        resolver: FunctionWrapper<R>,
        override val description : String?,
        override val isDeprecated: Boolean,
        override val deprecationReason: String?,
        inputValues : List<InputValueDef<*>> = emptyList()
) : BaseOperationDef<R>(name, resolver, inputValues), DescribedDef
