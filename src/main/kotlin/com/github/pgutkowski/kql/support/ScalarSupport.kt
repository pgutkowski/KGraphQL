package com.github.pgutkowski.kql.support

/**
 * Scalar resolves to a single support object, and can't have sub-selections in the query.
 * ScalarSupport defines strategy of handling supported support type
 */
interface ScalarSupport<T, in S> : ClassSupport<T>{

    /**
     * strategy for support serialization
     */
    fun serialize(input : S) : T

    /**
     * strategy for support deserialization
     */
    fun deserialize(input : T) : ByteArray

    /**
     * strategy for validation of serialized representation of support, returns true if input is valid
     * @throws com.pgutkowski.kql.ValidationException
     */
    fun validate(input : S): Boolean

}

