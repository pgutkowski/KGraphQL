package com.github.pgutkowski.kql.result

import com.github.pgutkowski.kql.request.Request


class Result(val request: Request?, val data : Map<String, Any?>?, val errors: Errors?)