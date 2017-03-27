package com.github.pgutkowski.kql

import com.github.pgutkowski.kql.annotation.method.ResolvingFunction
import com.github.pgutkowski.kql.resolve.MutationResolver
import com.github.pgutkowski.kql.resolve.QueryResolver
import com.github.pgutkowski.kql.schema.impl.DefaultSchema
import com.github.pgutkowski.kql.schema.impl.DefaultSchemaBuilder
import junit.framework.Assert.assertNotNull
import org.junit.Test


class SchemaCreationTest {

    @Test
    fun testBasicCreation(){
        KQL.newSchema()
                .addInput(TestClasses.InputClass::class)
                .addQueryField(TestClasses.Film::class, listOf(object: QueryResolver<TestClasses.Film> {}))
                .addQueryField(TestClasses.WithCollection::class, listOf(object: QueryResolver<TestClasses.WithCollection> {}))
                .build()
    }

    @Test
    fun testMutation(){
        val schema = DefaultSchemaBuilder()
                .addMutations(object : MutationResolver {
                    @ResolvingFunction
                    fun createUser(id: String, name: String): String {
                        return id
                    }

                    //not mutation
                    fun notMutation(): Int {
                        return 0
                    }
                })
                .build()

        (schema as DefaultSchema)

        assertNotNull(schema.mutations.any {
            it.functions.size == 1 && it.functions.any {
                function -> function.name == "createUser"
            }
        })
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidQueryWithoutResolver(){
        KQL.newSchema().addQueryField(TestClasses.Film::class, emptyList()).build()
    }
}