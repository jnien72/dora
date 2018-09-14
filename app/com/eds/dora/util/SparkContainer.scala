package com.eds.dora.util

import com.eds.dora.cluster.util.InstanceUtils
import org.apache.commons.logging.LogFactory
import org.apache.spark.sql.Row
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.types.{DataType, StringType, StructField, StructType}
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.LoggerFactory

trait SparkContainer {

  private val LOG = LoggerFactory.getLogger(getClass)

  private var sparkContext: SparkContext = null

  def getSparkContext(): SparkContext = {
    GlobalSync.get("spark").synchronized {
      if (sparkContext == null) {
        LOG.info("Starting Spark Context")
        val conf = new SparkConf()

        //insert instance configuration related to spark
        InstanceUtils.getInstanceConfiguration().foreach(entry => {
          val (key, value) = entry
          if (key.startsWith("spark.")) {
            conf.set(key, value)
          }
        })
        //manage scheduler
        val fairSchedulerRs = getClass.getResource("/spark-scheduler.xml")
        if (fairSchedulerRs != null) {
          conf.set("spark.scheduler.allocation.file", fairSchedulerRs.getFile)
        }
        if (conf.get("spark.master").startsWith("yarn")) {
          conf.set("spark.yarn.jars", System.getProperty("user.dir") + "/lib/*.jar")
        }
        conf.set("spark.sql.parquet.mergeSchema", "true")
        conf.set("javax.jdo.option.ConnectionDriverName", "org.apache.derby.jdbc.EmbeddedDriver")
        conf.set("javax.jdo.option.ConnectionURL", "jdbc:derby:memory:dora;create=true")
        conf.set("javax.jdo.option.ConnectionUserName", "APP")
        conf.set("javax.jdo.option.ConnectionPassword", "mine")
        conf.set("spark.ui.port", InstanceUtils.getSparkUiPort().toString)

        conf.setAppName(InstanceUtils.getInstanceName())

        sparkContext = new SparkContext(conf)
        LOG.info("Spark Context is ready")
      }
    }
    sparkContext
  }

  private var defaultHiveContext: HiveContext = null

  def getDefaultHiveContext(): HiveContext = {
    this.synchronized {
      if (defaultHiveContext == null) {
        LogFactory.getLog(getClass).info("Loading Default hive Context ...")

        defaultHiveContext = new HiveContext(getSparkContext)
        defaultHiveContext.setConf("context.name", "default")
        registerUDFs(defaultHiveContext)
      }
    }
    defaultHiveContext
  }

  def createSessionWithViews(tables: Set[String]): HiveContext = {
    val session = getDefaultHiveContext().newSession()
    registerUDFs(session)
    registerViews(session, tables)
    session
  }

  def registerViews(session: HiveContext, tables: Set[String]): Unit = {
    TableInfoCache.get().filter(entry => {
      val name = entry._1
      tables.contains(name)
    }).foreach(entry => {
      val info = entry._2
      if (info.fields.isEmpty || HdfsUtils.isDirectory(info.path)) {
        val (name, path) = (info.name, info.path)
        session.read.parquet(path).createOrReplaceTempView(name)
      } else {
        val fields = info.fields.get.map(field => {
          new StructField(field, StringType, true)
        })
        val schema = new StructType(fields)
        session.createDataFrame(getSparkContext.emptyRDD[Row], schema).createOrReplaceTempView(info.name)
        info.fields
      }
    })
  }

  def registerUDFs(hiveContext: HiveContext) = {
    HiveUtils.getDefinedUdfs().par.foreach(x => {
      LOG.info("Registering UDF '" + x._1 + "'")
      val method = hiveContext.udf.getClass.getMethod("register", classOf[String], x._3, classOf[DataType])
      method.invoke(hiveContext.udf, x._1, x._2.asInstanceOf[Object], x._4)
    })
  }
}