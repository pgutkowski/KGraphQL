package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.dsl.SchemaBuilder
import kotlin.reflect.KClass


class KGraphQL {
    companion object {
        fun schema(init : SchemaBuilder<Unit>.() -> Unit) = schema(Unit::class, init)

        /**
         * accepts instance of [KClass], instead of reified generic to avoid method signature clash
         */
        fun <Context : Any> schema(contextClass: KClass<Context>, init : SchemaBuilder<Context>.() -> Unit) : Schema<Context> {
            return SchemaBuilder(init).build(contextClass)
        }
    }
}