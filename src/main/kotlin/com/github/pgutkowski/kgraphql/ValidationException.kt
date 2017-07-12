package com.github.pgutkowski.kgraphql


class ValidationException(message: String, cause: Throwable? = null) : RequestException(message, cause)