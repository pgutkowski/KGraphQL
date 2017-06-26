package com.github.pgutkowski.kgraphql.schema.model

class KQLQuery<R> (
        name : String,
        resolver: FunctionWrapper<R>,
        override val description : String? = null,
        override val isDeprecated: Boolean = false,
        override val deprecationReason: String? = null
) : BaseKQLOperation<R>(name, resolver), DescribedKQLObject