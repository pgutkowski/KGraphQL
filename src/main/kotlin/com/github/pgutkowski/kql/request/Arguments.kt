package com.github.pgutkowski.kql.request


class Arguments () : HashMap<String, String>(){
    constructor(vararg pairs:  Pair<String, String>) : this(){
        this.putAll(pairs)
    }

    constructor(map : Map<String, String>) : this(){
        this.putAll(map)
    }
}