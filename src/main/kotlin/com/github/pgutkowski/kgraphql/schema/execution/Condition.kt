package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode

/**
 * wrapper for execution condition declared in query document
 */
sealed class Condition {
    /**
     * conditional directive is instruction for engine whether exclude marked field
     */
    class Directive(functionWrapper: FunctionWrapper<Boolean>) : FunctionWrapper<Boolean> by functionWrapper, Condition()

    /**
     * type conditions can be declared on fragments
     */
    class Type(val schemaNode : SchemaNode.Type) : Condition()
}
