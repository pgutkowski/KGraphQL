package com.github.pgutkowski.kgraphql.result

import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.request.Request


class Result(val request: Request?, val graph : Graph, val data : Map<String, Any?>?, val errors: Errors?)