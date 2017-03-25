package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.support.QueryResolver
import org.junit.Test


class DefaultSchemaTest {

    val testedSchema = DefaultSchemaBuilder()
            .addInput(TestClasses.InputClass::class)
            .addQuery(TestClasses.QueryClass::class, listOf(object:  QueryResolver<TestClasses.QueryClass> {}))
            .build()

    @Test
    fun testBasicQuery(){
        testedSchema.handleQuery("{queryClass{title}}")
    }
}