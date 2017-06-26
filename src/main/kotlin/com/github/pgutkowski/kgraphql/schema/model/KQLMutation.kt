package com.github.pgutkowski.kgraphql.schema.model

class KQLMutation<R> (
        name : String,
        resolver: FunctionWrapper<R>,
        override val description : String?,
        override val isDeprecated: Boolean,
        override val deprecationReason: String?
) : BaseKQLOperation<R>(name, resolver), DescribedKQLObject
