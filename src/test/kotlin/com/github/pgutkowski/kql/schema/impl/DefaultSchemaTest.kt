package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.support.ClassSupport
import org.junit.Test


class DefaultSchemaTest {

    val testedSchema = DefaultSchemaBuilder()
            .addSupportedClass(TestClasses.InputClass::class, object: ClassSupport<TestClasses.InputClass> {})
            .addClass(TestClasses.QueryClass::class)
            .addClass(TestClasses.WithCollection::class)
            .build()

    @Test
    fun testBasicQuery(){
        testedSchema.handleQuery("{queryClass{title}}")
    }
}