package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.defaultSchema

@Specification("2.9 Input Values")
class InputValuesSpecificationTest {

    val schema = defaultSchema {
        query("Int") { resolver { value: Int -> value } }
    }
}