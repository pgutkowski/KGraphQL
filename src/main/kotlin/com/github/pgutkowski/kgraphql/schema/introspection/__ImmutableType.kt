package com.github.pgutkowski.kgraphql.schema.introspection


data class __ImmutableType(
        override val kind: __TypeKind,
        override val name: String?,
        override val description: String,
        override val fields: List<__Field>?,
        override val interfaces: List<__Type>?,
        override val possibleTypes: List<__Type>?,
        override val enumValues: List<__EnumValue>?,
        override val inputFields: List<__InputValue>?,
        override val ofType: __Type? = null
) : __Type {
    override fun toString() = this.asString()
}
