package com.github.pgutkowski.kgraphql.schema.introspection

import com.github.pgutkowski.kgraphql.schema.structure2.LookupSchema
import com.github.pgutkowski.kgraphql.schema.structure2.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType


class SchemaProxy<Context: Any>(var proxiedSchema : LookupSchema<Context>? = null) : LookupSchema<Context> {

    companion object {
        const val ILLEGAL_STATE_MESSAGE = "Missing proxied __Schema instance"
    }

    private fun getProxied() = proxiedSchema ?: throw IllegalStateException(ILLEGAL_STATE_MESSAGE)

    override val types: List<__Type>
        get() = getProxied().types

    override val queryType: __Type
        get() = getProxied().queryType

    override val mutationType: __Type?
        get() = getProxied().mutationType

    override val subscriptionType: __Type?
        get() = getProxied().subscriptionType

    override val directives: List<__Directive>
        get() = getProxied().directives

    override fun findTypeByName(name: String): __Type? = getProxied().findTypeByName(name)

    override fun typeByKClass(kClass: KClass<*>): Type? = getProxied().typeByKClass(kClass)

    override fun typeByKType(kType: KType): Type? = typeByKType(kType)

    override fun typeByName(name: String): Type? = typeByName(name)

    override fun inputTypeByKClass(kClass: KClass<*>): Type? = inputTypeByKClass(kClass)

    override fun inputTypeByKType(kType: KType): Type? = inputTypeByKType(kType)

    override fun inputTypeByName(name: String): Type? = inputTypeByName(name)

    override fun execute(request: String, variables: String?, context: Context?): String {
        return getProxied().execute(request, variables, context)
    }
}
