package com.github.pgutkowski.kgraphql.graph

import com.github.pgutkowski.kgraphql.request.Arguments


data class DirectiveInvocation(val key : String, val arguments: Arguments? = null)