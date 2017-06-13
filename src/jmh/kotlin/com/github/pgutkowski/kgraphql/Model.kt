package com.github.pgutkowski.kgraphql


data class ModelOne(val name : String, val quantity : Int = 1, val active : Boolean = true)

data class ModelTwo(val one : ModelOne, val range: IntRange)

data class ModelThree(val id : String, val twos : List<ModelTwo>)