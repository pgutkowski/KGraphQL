package com.github.pgutkowski.kgraphql.result

import com.github.pgutkowski.kgraphql.request.Request


class Result(val request: Request?, val data : Map<String, Any?>?, val errors: Errors?)