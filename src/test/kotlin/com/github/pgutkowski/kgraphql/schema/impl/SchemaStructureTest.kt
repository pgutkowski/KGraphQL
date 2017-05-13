package com.github.pgutkowski.kgraphql.schema.impl

import org.junit.Test

class SchemaStructureTest : BaseSchemaTest() {

    @Test
    fun testSimpleStructure(){
        val schemaStructure = SchemaStructure.of(testedSchema)
    }
}
