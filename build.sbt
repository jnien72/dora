name := "dora"

version := "1.0"

lazy val `dora` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers += "hwx-releases" at "http://repo.hortonworks.com/content/repositories/releases/"
resolvers += "hwx-public" at "http://repo.hortonworks.com/content/groups/public/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

logLevel := Level.Error

libraryDependencies ++= Seq(
  "com.healthmarketscience.rmiio" % "rmiio" % "2.1.2",
  "com.101tec" % "zkclient" % "0.10",
  "org.apache.zookeeper"%"zookeeper"%"3.4.6",
  "org.apache.spark" % "spark-core_2.11" % "2.1.1",
  "org.apache.spark" % "spark-yarn_2.11" % "2.1.1",
  "org.apache.spark" % "spark-sql_2.11" % "2.1.1",
  "org.apache.spark" % "spark-hive_2.11" % "2.1.1",
  "org.apache.spark" % "spark-mllib_2.11" % "2.1.1",
  "org.apache.hadoop" % "hadoop-client" % "2.7.7",
  "org.apache.hadoop" % "hadoop-common" % "2.7.7",
  "org.apache.hadoop" % "hadoop-hdfs" % "2.7.7",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.eclipse.jetty" % "jetty-server" % "9.4.6.v20170531" ,
  "org.eclipse.jetty" % "jetty-servlet" % "9.4.6.v20170531" ,
  "org.reflections" % "reflections" % "0.9.9-RC1",
  "com.google.guava" % "guava" % "16.0",
  "commons-codec" % "commons-codec" % "1.8",
  "com.jcraft" % "jsch" % "0.1.53",
  "org.apache.parquet" % "parquet-avro" % "1.7.0",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.4",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.4",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.8.4",
  "junit" % "junit" % "4.12"
)

libraryDependencies ~= {_
    .map(_.exclude("org.slf4j", "slf4j-log4j12"))
    .map(_.exclude("org.eclipse.jetty.aggregate", "jetty-all"))
    .map(_.exclude("javax.jms", "jms"))
    .map(_.exclude("com.sun.jdmk", "jmxtools"))
    .map(_.exclude("com.sun.jmx", "jmxri"))
    .map(_.exclude("javax.servlet", "servlet-api"))
    .map(_.exclude("javax.ws.rs", "jsr311-api"))
    .map(_.force())
}

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

updateOptions := updateOptions.value.withCachedResolution(true)

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case x if x.startsWith("META-INF") && x.endsWith(".SF") => MergeStrategy.discard
  case x if x.startsWith("META-INF") && x.endsWith(".RSA") => MergeStrategy.discard
  case x if x.startsWith("META-INF") && x.endsWith(".DSA") => MergeStrategy.discard
  case x if x.startsWith("META-INF") && x.endsWith(".TXT") => MergeStrategy.discard
  case x if x.startsWith("META-INF") && x.endsWith("org.apache.hadoop.fs.FileSystem") => MergeStrategy.concat
  case PathList(ps@_*) if ps.last endsWith ".conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}