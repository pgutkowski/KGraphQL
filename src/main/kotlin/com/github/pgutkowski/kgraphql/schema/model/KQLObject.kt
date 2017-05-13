package com.github.pgutkowski.kgraphql.schema.model

/**
 * KQLObject represents part of KGraphQL schema. Subclasses represent schema elements stated in GraphQL spec.
 * KQLObject's name should be unique in scope of schema
 */
abstract class KQLObject(val name : String)