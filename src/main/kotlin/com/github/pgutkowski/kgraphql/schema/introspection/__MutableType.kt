package com.github.pgutkowski.kgraphql.schema.introspection


data class __MutableType (
        override val kind: __TypeKind,
        override val name: String?,
        override val description: String,
        val mutableFields: MutableList<__Field> = mutableListOf(),
        override val interfaces: List<__Type>?,
        override val possibleTypes: List<__Type>?,
        override val enumValues: List<__EnumValue>?,
        val mutableInputFields: MutableList<__InputValue> = mutableListOf(),
        override val ofType: __Type? = null
) : __Type {
    //__Type#fields contract is that it returns null if no fields are present
    override val fields: List<__Field>?
        get() = if(mutableFields.isEmpty()) null else mutableFields

    //__Type#inputFields contract is that it returns null if no fields are present
    override val inputFields: List<__InputValue>?
        get() = if(mutableInputFields.isEmpty()) null else mutableInputFields
}