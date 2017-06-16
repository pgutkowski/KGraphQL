package com.github.pgutkowski.kgraphql.request

import com.github.benmanes.caffeine.cache.Caffeine


class CachingDocumentParser(cacheMaximumSize : Long = 1000L) : DocumentParser() {
    
    sealed class Result {
        class Success(val operations :List<Operation>) : Result()
        class Exception(val exception: kotlin.Exception) : Result()
    }

    val cache = Caffeine.newBuilder().maximumSize(cacheMaximumSize).build<String, Result>()

    override fun parseDocument(input: String): List<Operation> {
        val result =  cache.get(input, {
            try {
                Result.Success(super.parseDocument(input))
            } catch ( e : Exception){
                Result.Exception(e)
            }
        })

        when (result) {
            is Result.Success -> return result.operations
            is Result.Exception -> throw result.exception
            else -> {
                cache.invalidateAll()
                throw IllegalStateException("Internal error of CachingDocumentParser")
            }
        }
    }
}