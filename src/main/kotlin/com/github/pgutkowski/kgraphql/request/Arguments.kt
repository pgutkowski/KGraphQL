package com.github.pgutkowski.kgraphql.request

/**
 * Represents raw values of arguments in query document, so (id: 343, name: "Pablo", book: $book) is
 * equal to {"id" to "343", "name" to "Pablo" and "book" to "$book"}
 */
class Arguments () : HashMap<String, Any>(){
    constructor(vararg pairs:  Pair<String, Any>) : this(){
        this.putAll(pairs)
    }
}