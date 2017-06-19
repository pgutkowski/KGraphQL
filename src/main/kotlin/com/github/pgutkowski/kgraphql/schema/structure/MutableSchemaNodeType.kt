package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.schema.model.KQLType

/**
 * MutableType allows to avoid circular reference issues when building schema.
 * @see getType
 * @see objectTypeNodes
 */
class MutableSchemaNodeType(
        kqlObjectType: KQLType.Kotlin<*>,
        val mutableProperties: MutableMap<String, Property> = mutableMapOf(),
        val mutableUnionProperties: MutableMap<String, UnionProperty> = mutableMapOf()
) : SchemaNode.Type(kqlObjectType, mutableProperties, mutableUnionProperties)