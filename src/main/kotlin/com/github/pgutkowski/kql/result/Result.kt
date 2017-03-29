package com.github.pgutkowski.kql.result

import com.github.pgutkowski.kql.Graph
import com.github.pgutkowski.kql.request.Request


class Result(val request: Request?, val data : Graph?, val errors: Errors?)