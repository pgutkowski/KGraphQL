package com.github.pgutkowski.kgraphql.schema.introspection


enum class __TypeKind {
    SCALAR,
    OBJECT,
    INTERFACE,
    UNION,
    ENUM,
    INPUT_OBJECT,

    //wrapper types
    LIST,
    NON_NULL
}