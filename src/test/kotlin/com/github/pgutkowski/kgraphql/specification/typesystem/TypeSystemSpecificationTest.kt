package com.github.pgutkowski.kgraphql.specification.typesystem

import com.github.pgutkowski.kgraphql.KGraphQL.Companion.schema
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.schema.SchemaException
import org.junit.Test

@Specification("3 Type System")
class TypeSystemSpecificationTest {

    class String

    class __Type

    @Test
    fun `All types within a GraphQL schema must have unique names`(){
        expect<SchemaException>("Cannot add Object type with duplicated name String"){
            schema {
                type<TypeSystemSpecificationTest.String>()
            }
        }
    }

    @Test
    fun `All types and directives defined within a schema must not have a name which begins with "__"`(){
        expect<SchemaException>("Type name starting with \"__\" are excluded for introspection system"){
            schema {
                type<__Type>()
            }
        }
    }
}
