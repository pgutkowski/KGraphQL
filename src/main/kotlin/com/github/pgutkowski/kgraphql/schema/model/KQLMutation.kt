package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper

class KQLMutation<R> (
        name : String,
        resolver: FunctionWrapper<R>,
        override val description : String?
) : BaseKQLOperation<R>(name, resolver), DescribedKQLObject
