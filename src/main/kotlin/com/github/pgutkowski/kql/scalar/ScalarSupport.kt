package com.github.pgutkowski.kql.scalar

/**
 * Scalar resolves to a single scalar object, and can't have sub-selections in the request.
 * ScalarSupport defines strategy of handling supported scalar type
 */
interface ScalarSupport<T> {

    /**
     * strategy for scalar serialization
     */
    fun serialize(input : String) : T

    /**
     * strategy for scalar deserialization
     */
    fun deserialize(input : T) : String

    /**
     * strategy for validation of serialized representation of scalar, returns true if input is valid
     * @throws com.pgutkowski.kql.ValidationException
     */
    fun validate(input : String): Boolean

}

