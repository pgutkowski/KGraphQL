package com.github.pgutkowski.kgraphql.request


class Arguments () : HashMap<String, String>(){
    constructor(vararg pairs:  Pair<String, String>) : this(){
        this.putAll(pairs)
    }

    constructor(map : Map<String, String>) : this(){
        this.putAll(map)
    }
}