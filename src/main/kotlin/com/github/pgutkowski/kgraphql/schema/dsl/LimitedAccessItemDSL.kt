package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.Context


abstract class LimitedAccessItemDSL<PARENT> : DepreciableItemDSL() {

    internal var accessRuleBlock: ((PARENT?, Context) -> Exception?)? = null

//    fun accessRule(rule: (PARENT?, Context) -> Exception?){
//        this.accessRuleBlock = rule
//    }
}