package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.TestClasses
import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test
import java.util.*


class SchemaInternalsTest : BaseSchemaTest() {

    @Test
    fun testBasicResult(){
        val result = testedSchema.createResult("{film{title}}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(prestige))
    }

    @Test
    fun testBasicResultWithImpliedFields(){
        val result = testedSchema.createResult("{film}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(prestige))
    }

    @Test
    fun testNamedBasicResult(){
        val result = testedSchema.createResult("query Named {film{title}}")
        assertThat(result.errors, nullValue())
        assertThat(result.data!!["film"] as TestClasses.Film, equalTo(prestige))
    }

    @Test(expected = SyntaxException::class)
    fun testInvalidQueryMissingBracket(){
        testedSchema.createResult("query Named {film{title}")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBasicInvalidNamedQuery(){
        testedSchema.createResult("InvalidNamedQuery {film{title}}")
    }

    @Test
    fun testDSLCreatedUUIDScalarSupport(){
        val uuidScalar = testedSchema.scalars.find { it.name == "UUID" }!!.scalarSupport as ScalarSupport<UUID>
        val testUuid = UUID.randomUUID()
        assertThat(uuidScalar.deserialize(testUuid), equalTo(testUuid.toString()))
        assertThat(uuidScalar.serialize(testUuid.toString()), equalTo(testUuid))
        assertThat(uuidScalar.validate(testUuid.toString()), equalTo(true))
    }
}