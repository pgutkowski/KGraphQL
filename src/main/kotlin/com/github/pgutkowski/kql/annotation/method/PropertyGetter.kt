package com.github.pgutkowski.kql.annotation.method

/**
 * Denotes FieldResolver's method, which is used of fetch/transform given property data
 */
annotation class PropertyGetter(
        /**
         * "" as default value of propertyName is a bit dirty, but kotlin does not scalar nullable properties in annotations
         */
        val propertyName : String = ""
)
