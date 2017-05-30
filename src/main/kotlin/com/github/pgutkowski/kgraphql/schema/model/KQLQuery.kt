package com.github.pgutkowski.kgraphql.schema.model

class KQLQuery<R> (
        name : String,
        resolver: FunctionWrapper<R>,
        override val description : String?
) : BaseKQLOperation<R>(name, resolver), DescribedKQLObject