package com.github.pgutkowski.kgraphql.request

/**
 * Represents variable in scope of operation, in 'query ($var: Int = 33)',
 * '$var' is name, 'String' is type and '33' is default value
 */
data class Variable(val name : String, val type: String, val defaultValue : String?)