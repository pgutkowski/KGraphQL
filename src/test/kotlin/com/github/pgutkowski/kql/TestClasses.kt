package com.github.pgutkowski.kql

import com.github.pgutkowski.kql.annotation.type.Input
import com.github.pgutkowski.kql.annotation.type.Query
import com.github.pgutkowski.kql.scalar.ScalarSupport


class TestClasses {

    @Query
    class Film(val year: Int, val title: String, val director: Director)

    class Director(val name : String, val age: Int, val favActors: List<String>)

    class Scalar(val id : String)

    class ScalarTestClassSupport : ScalarSupport<Scalar> {
        override fun serialize(input: String): Scalar = Scalar(input)
        override fun deserialize(input: Scalar): ByteArray = input.id.toByteArray()
        override fun validate(input: String) : Boolean = input.isNotEmpty()
    }

    @Input
    class InputClass

    @Query
    class WithCollection(val cases: List<String>)

    @Query
    class WithNullableCollection(val cases: List<String?>)
}