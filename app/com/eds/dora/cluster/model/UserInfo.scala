package com.eds.dora.cluster.model

//acl content: namespace->accessible components
case class UserInfo(var username:String,var password:String,var acl:Map[String,List[String]])