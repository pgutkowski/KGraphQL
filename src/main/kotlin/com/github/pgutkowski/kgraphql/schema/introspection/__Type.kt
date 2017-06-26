
package com.github.pgutkowski.kgraphql.schema.introspection

/**
 * GraphQL introspection system defines __Type to represent all of TypeKinds
 * If some field does not apply to given type, it returns null
 */
interface __Type {
    val kind: __TypeKind
    val name : String?
    val description : String
    //OBJECT and INTERFACE only
    val fields : List<__Field>?
    //OBJECT only
    val interfaces : List<__Type>?
    //INTERFACE and UNION only
    val possibleTypes : List<__Type>?
    //ENUM only
    val enumValues : List<__EnumValue>?
    //INPUT_OBJECT only
    val inputFields : List<__InputValue>?
    //NON_NULL and LIST only
    val ofType : __Type?
}