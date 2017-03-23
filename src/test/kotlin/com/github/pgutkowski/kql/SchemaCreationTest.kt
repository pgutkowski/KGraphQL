package com.github.pgutkowski.kql

import com.github.pgutkowski.kql.support.ClassSupport
import org.junit.Test


class SchemaCreationTest {

    @Test
    fun testBasicCreation(){
        val schema = KQL.newSchema()
                .addSupportedClass(TestClasses.InputClass::class, object: ClassSupport<TestClasses.InputClass> {})
                .addClass(TestClasses.QueryClass::class)
                .addClass(TestClasses.WithCollection::class)
                .build()
    }
}