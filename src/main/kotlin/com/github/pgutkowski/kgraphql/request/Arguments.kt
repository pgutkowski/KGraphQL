package com.github.pgutkowski.kgraphql.request


class Arguments () : HashMap<String, Any>(){
    constructor(vararg pairs:  Pair<String, String>) : this(){
        this.putAll(pairs)
    }

    constructor(map : Map<String, Any>) : this(){
        this.putAll(map)
    }
}