package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.deserialize
import com.github.pgutkowski.kql.extract
import junit.framework.Assert
import org.junit.Test


class MutationSchemaTest : BaseSchemaTest() {

    @Test
    fun testCollectionEntriesProperties(){
        val result = testedSchema.handleRequest("mutation {createActor(name: \"Michael Caine\", age: 72)}")
        val map = deserialize(result)
        assertNoErrors(map)
    }
}