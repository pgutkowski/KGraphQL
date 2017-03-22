package com.github.pgutkowski.kql.scalar

/**
 * Scalar resolves to a single scalar object, and can't have sub-selections in the query.
 * ScalarSupport defines strategy of handling supported scalar type
 */
interface ScalarSupport<O, in I> {

    /**
     * strategy for scalar serialization
     */
    fun serialize(input : I) : O

    /**
     * strategy for scalar deserialization
     */
    fun deserialize(input : O) : ByteArray

    /**
     * strategy for validation of serialized representation of scalar, returns true if input is valid
     * @throws com.pgutkowski.kql.ValidationException
     */
    fun validate(input : I): Boolean

}

