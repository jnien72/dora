package com.eds.dora.cluster.model

case class NamespaceInfo(var name: String,var enabled:Boolean,var instances: List[InstanceInfo],var predefinedTables: Map[String, String])