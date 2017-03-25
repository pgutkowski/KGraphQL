package com.github.pgutkowski.kql

import com.github.pgutkowski.kql.support.QueryResolver
import org.junit.Test


class SchemaCreationTest {

    @Test
    fun testBasicCreation(){
        KQL.newSchema()
                .addInput(TestClasses.InputClass::class)
                .addQuery(TestClasses.QueryClass::class, listOf(object: QueryResolver<TestClasses.QueryClass> {}))
                .addQuery(TestClasses.WithCollection::class, listOf(object: QueryResolver<TestClasses.WithCollection> {}))
                .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidQueryWithoutResolver(){
        KQL.newSchema().addQuery(TestClasses.QueryClass::class, emptyList()).build()
    }
}