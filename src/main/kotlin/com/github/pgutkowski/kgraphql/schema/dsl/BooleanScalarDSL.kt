package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.scalar.BooleanScalarCoercion
import com.github.pgutkowski.kgraphql.schema.scalar.ScalarCoercion
import kotlin.reflect.KClass


class BooleanScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T, Boolean>.() -> Unit)
    : ScalarDSL<T, Boolean>(kClass, block){

    override fun createCoercionFromFunctions(): ScalarCoercion<T, Boolean> {
        return object : BooleanScalarCoercion<T> {

            val serializeImpl = serialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            val deserializeImpl = deserialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            override fun serialize(instance: T): Boolean = serializeImpl(instance)

            override fun deserialize(raw: Boolean): T = deserializeImpl(raw)
        }
    }
}