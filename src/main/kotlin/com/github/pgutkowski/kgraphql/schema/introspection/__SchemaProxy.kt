package com.github.pgutkowski.kgraphql.schema.introspection


class __SchemaProxy : __Schema {

    companion object {
        const val ILLEGAL_STATE_MESSAGE = "Missing proxied __Schema instance"
    }

    var proxiedSchema : __Schema? = null

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

    override fun findTypeByName(name: String): __Type? {
        return getProxied().findTypeByName(name)
    }
}