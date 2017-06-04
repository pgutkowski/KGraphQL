package com.github.pgutkowski.kgraphql.schema.directive

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper


class DirectiveExecution(val function: FunctionWrapper<DirectiveResult>) : FunctionWrapper<DirectiveResult> by function