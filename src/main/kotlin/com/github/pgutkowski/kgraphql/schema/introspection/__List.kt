package com.github.pgutkowski.kgraphql.schema.introspection


class __List(override val ofType: __Type?) : __Type{

    override val kind: __TypeKind = __TypeKind.LIST

    override val name: String? = null

    override val description: String = "List wrapper type"

    override val fields: List<__Field>? = null

    override val interfaces: List<__Type>? = null

    override val possibleTypes: List<__Type>? = null

    override val enumValues: List<__EnumValue>? = null

    override val inputFields: List<__InputValue>? = null

    override fun toString(): String = "__List(ofType=$ofType)"
}