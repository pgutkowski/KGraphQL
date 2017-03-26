package com.github.pgutkowski.kql.request


class Graph() : HashMap<String, Any?>(){
    constructor(vararg pairs:  Pair<String, Any?>) : this(){
        this.putAll(pairs)
    }

    constructor(map : Map<String, Any?>) : this(){
        this.putAll(map)
    }
}