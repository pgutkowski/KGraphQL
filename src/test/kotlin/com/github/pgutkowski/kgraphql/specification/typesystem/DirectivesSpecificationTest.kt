package com.github.pgutkowski.kgraphql.specification.typesystem

import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.extractOrNull
import com.github.pgutkowski.kgraphql.integration.BaseSchemaTest
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("3.2 Directives")
class DirectivesSpecificationTest : BaseSchemaTest() {

    @Test
    fun `query with @include directive on field`(){
        val map = execute("{film{title, year @include(if: false)}}")
        assertThat(extractOrNull(map, "data/film/year"), nullValue())
    }

    @Test
    fun `query with @skip directive on field`(){
        val map = execute("{film{title, year @skip(if: true)}}")
        assertThat(extractOrNull(map, "data/film/year"), nullValue())
    }

    @Test
    fun `query with @include and @skip directive on field`(){
        val mapBothSkip = execute("{film{title, year @include(if: false) @skip(if: true)}}")
        assertThat(extractOrNull(mapBothSkip, "data/film/year"), nullValue())

        val mapOnlySkip = execute("{film{title, year @include(if: true) @skip(if: true)}}")
        assertThat(extractOrNull(mapOnlySkip, "data/film/year"), nullValue())

        val mapOnlyInclude = execute("{film{title, year @include(if: false) @skip(if: false)}}")
        assertThat(extractOrNull(mapOnlyInclude, "data/film/year"), nullValue())

        val mapNeither = execute("{film{title, year @include(if: true) @skip(if: false)}}")
        assertThat(extractOrNull(mapNeither, "data/film/year"), notNullValue())
    }

    @Test
    fun `query with @include directive on field with variable`(){
        val map = execute(
                "query film (\$include: Boolean!) {film{title, year @include(if: \$include)}}",
                "{\"include\":\"false\"}"
        )
        assertThat(extractOrNull(map, "data/film/year"), nullValue())
    }
}