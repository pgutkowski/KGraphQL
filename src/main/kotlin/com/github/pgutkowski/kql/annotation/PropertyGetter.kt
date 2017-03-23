package com.github.pgutkowski.kql.annotation

/**
 * PropertyGetter denotes function in ClassSupport instance which is used to fetch/transform given property data
 */
annotation class PropertyGetter(
        /**
         * "" as default value of propertyName is a bit dirty, but kotlin does not support nullable properties in annotations
         */
        val propertyName : String = ""
)
