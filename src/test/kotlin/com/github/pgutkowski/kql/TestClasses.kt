package com.github.pgutkowski.kql

import com.github.pgutkowski.kql.annotation.type.Input
import com.github.pgutkowski.kql.annotation.method.Mutation
import com.github.pgutkowski.kql.annotation.type.Query
import com.github.pgutkowski.kql.scalar.ByteArrayScalarSupport


class TestClasses {

    @Query
    class QueryClass(val year: Int, val title: String)

    class Scalar(val id : String)

    class ScalarTestClassSupport : ByteArrayScalarSupport<Scalar> {
        override fun serialize(input: ByteArray): Scalar = Scalar(input.toString(Charsets.UTF_16))
        override fun deserialize(input: Scalar): ByteArray = input.id.toByteArray()
        override fun validate(input: ByteArray) : Boolean = input.isNotEmpty()
    }

    @Input
    class InputClass

    @Query
    class WithCollection(val cases: List<String>)

    @Query
    class WithNullableCollection(val cases: List<String?>)
}