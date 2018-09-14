package com.eds.dora.query.service

import java.io.{InputStream, PrintWriter}
import java.rmi.registry.LocateRegistry
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.eds.dora.cluster.util.InstanceUtils
import com.eds.dora.dao.{MetaDao, MetaTable}
import com.eds.dora.query.model.{QueryResponse, QueryStatus}
import com.eds.dora.query.utils.TableMetaUtils
import com.eds.dora.util._
import com.google.common.cache.CacheBuilder
import com.healthmarketscience.rmiio.SerializableInputStream
import org.apache.spark.JobExecutionStatus
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.hive.HiveContext
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, TimeoutException}

object QueryServiceImpl extends QueryService with SparkContainer {

  private val runningJobs = new ConcurrentHashMap[String, Boolean]()
  private val jobHistory = CacheBuilder.newBuilder().maximumSize(500).expireAfterWrite(1, TimeUnit.DAYS).build[String, QueryStatus]()
  private val TMP_DIR = EnvProperty.get(EnvConstants.TMP_DIR)

  private val tableLastUpdateTimeCache = mutable.Map[String, Long]()
  val LOG = LoggerFactory.getLogger(getClass)

  def init(): Unit = {
    LoggerFactory.getLogger(getClass).info("Setting up " + this.getClass.getSimpleName)

    getSparkContext()
    TableInfoCache.setup(this)
    val currentTime = System.currentTimeMillis()
    TableInfoCache.get().foreach(tableInfo => {
      tableLastUpdateTimeCache += (tableInfo._1 -> currentTime)
    })

    System.setProperty("java.rmi.server.hostname", NetworkUtils.getHostname())
    val stub = UnicastRemoteObject.exportObject(this, 0)
    val registry = LocateRegistry.createRegistry(InstanceUtils.getServicePort())
    val connStr = "//" + NetworkUtils.getHostname() + ":" + InstanceUtils.getServicePort() + "/" + classOf[QueryService].getName
    registry.rebind(connStr, stub)
    LOG.info("Listening on " + connStr)
  }

  private def refreshUpdatedDepTables(sql: String, hivecontext: HiveContext): Unit = {
    val tableInfo = TableInfoCache.get()
    SqlUtils.extractTablesFromQuery(sql).foreach(dep => {
      val depTableInfo = tableInfo.get(dep)
      val nowTimestamp = System.currentTimeMillis()
      if (depTableInfo.isDefined) {
        if (tableLastUpdateTimeCache.get(dep).isEmpty) {
          val lastTxTime = if (depTableInfo.get.lastTxDate.isDefined && AssertUtils.hasValue(depTableInfo.get.lastTxDate.get)) DateTimeUtils.getMillisFromDateTimeExp(depTableInfo.get.lastTxDate.get, "yyyy-MM-dd HH:mm:ss") else -1
          tableLastUpdateTimeCache += (dep -> lastTxTime)
        }
        var requiredUpdate = false
        val lastUpdatedTimestamp = tableLastUpdateTimeCache.get(dep).get
        if (depTableInfo.get.lastTxDate.isEmpty) {
          // updated every hour for predefined table
          val bufferMin = 5
          var checkTimestamp = nowTimestamp / 3600000 * 3600000 + 60 * 1000 * bufferMin
          if (checkTimestamp > nowTimestamp) {
            checkTimestamp -= 3600000
          }
          requiredUpdate = checkTimestamp > lastUpdatedTimestamp && checkTimestamp < nowTimestamp
        } else {
          requiredUpdate = depTableInfo.get.lastUpdate.get > lastUpdatedTimestamp
        }
        if (requiredUpdate) {
          val depPath = depTableInfo.get.path
          try {
            hivecontext.dropTempTable(dep)
          } catch {
            case t: Throwable =>
          }
          hivecontext.read.parquet(depPath).createOrReplaceTempView(dep)
          tableLastUpdateTimeCache += (dep -> nowTimestamp)
        }
      } else {
        throw new RuntimeException("Table [" + dep + "] does not exist")
      }
    })
  }

  override def query(user: String, jobId: String, sql: String, numOfRows: Int, txDate: String): QueryResponse = {
    LoggerFactory.getLogger(getClass).info("Start processing query request => " + JsonUtils.toJson(sql))
    val start = System.currentTimeMillis()
    val jobStatus = new QueryStatus()
    jobStatus.startMillis = start + ""
    jobStatus.jobId = jobId
    jobStatus.user = user
    jobStatus.sql = sql
    jobStatus.status = "running"
    jobHistory.put(jobId, jobStatus)

    var queryResponse: QueryResponse = null
    val queryTimeoutMillis = EnvProperty.get(EnvConstants.QUERY_TIMEOUT_MILLIS).toInt
    try {
      TimedBlockUtils.timeout[Unit](queryTimeoutMillis,
        Future[Unit] {
          try {
            runningJobs.put(jobId, true)
            val cmdSequence = sql.split(";(?=([^(\"|')]*(\"|')[^(\"|')]*(\"|'))*[^(\"|')]*$)")
              .map(x => x.trim).filter(x => x.trim.length > 0)
            var result: DataFrame = null
            cmdSequence.foreach(cmd => {
              HiveUtils.validateQueryCmd(cmd)
              getDefaultHiveContext().sparkContext.setJobGroup(jobId, cmd, interruptOnCancel = true)
              if (runningJobs.containsKey(jobId)) {
                val ts = if (AssertUtils.hasValue(txDate)) {
                  DateTimeUtils.getMillisFromDateTimeExp(txDate, "yyyy-MM-dd HH:mm:ss")
                } else {
                  System.currentTimeMillis()
                }
                val sql = TxDateUtils.parse(cmd, ts)
                refreshUpdatedDepTables(sql, getDefaultHiveContext())
                resolveQueryTables(sql)
                result = getDefaultHiveContext().sql(sql)
              }
            })
            var elapsed: String = ""
            if (result != null) {
              val dataTypes = result.queryExecution.analyzed.output.map(_.dataType).toArray
              val columns = result.dtypes.map(dtype => dtype._1.toString())
              val data = result.head(numOfRows).map(
                row => row.toSeq.toArray.zipWithIndex.map {
                  case (x, idx) => HiveUtils.toHiveString(x, dataTypes(idx)).replace("\t", "  ")
                }
              )
              if (!jobStatus.status.startsWith("cancelled")) {
                elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
                jobStatus.elapsed = elapsed
              }
              queryResponse = QueryResponse(jobStatus.elapsed, columns, data, null)
            } else {
              if (!jobStatus.status.startsWith("cancelled")) {
                elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
                jobStatus.elapsed = elapsed
              }
              queryResponse = QueryResponse(jobStatus.elapsed, null, null, null)
            }
            runningJobs.synchronized {
              if (runningJobs.containsKey(jobId)) {
                jobStatus.status = "success"
                if (elapsed.isEmpty) {
                  elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
                  jobStatus.elapsed = elapsed
                }
                LoggerFactory.getLogger(getClass) info ("Finished processing query '" + jobId + "'")
                runningJobs.remove(jobId)
              }
            }
          } catch {
            case t: TimeoutException =>
              jobStatus.elapsed = DateTimeUtils.getTimeDiffExp(start, start + queryTimeoutMillis)
              jobStatus.status = "timeout"
              LoggerFactory.getLogger(getClass).error("Timeout", t)
              throw t
          }
        }
      )
      queryResponse
    } catch {
      case t: TimeoutException =>
        throw new RuntimeException("Query execution time exceeded limit")
      case t: Throwable =>
        if (jobStatus.status.equals("running")) {
          jobStatus.status = "error"
          if (jobStatus.elapsed.isEmpty) {
            jobStatus.elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
          }
        }
        LoggerFactory.getLogger(getClass).error("Error", t)
        throw t
    }
  }

  override def queryAsStream(user: String, jobId: String, sql: String, numOfRows: Int, txDate: String): (Option[Array[String]], SerializableInputStream) = {
    LoggerFactory.getLogger(getClass).info("Start processing query request => " + JsonUtils.toJson(sql))

    val start = System.currentTimeMillis()
    val jobStatus = new QueryStatus()
    jobStatus.startMillis = start + ""
    jobStatus.jobId = jobId
    jobStatus.user = user
    jobStatus.sql = sql
    jobStatus.status = "running"
    jobHistory.put(jobId, jobStatus)

    val queryTimeoutMillis = EnvProperty.get(EnvConstants.QUERY_TIMEOUT_MILLIS).toInt
    var result: DataFrame = null
    var tmpPath: String = null
    val buffer = new CircularByteBuffer(32687)
    val writer = new PrintWriter(buffer.getOutputStream)
    var inputStream: InputStream = null
    try {
      TimedBlockUtils.timeout[Unit](queryTimeoutMillis,
        Future[Unit] {
          try {
            runningJobs.put(jobId, true)
            val cmdSequence = sql.split(";(?=([^(\"|')]*(\"|')[^(\"|')]*(\"|'))*[^(\"|')]*$)")
              .map(x => x.trim).filter(x => x.trim.length > 0)
            cmdSequence.foreach(cmd => {
              HiveUtils.validateQueryCmd(cmd)
              getDefaultHiveContext().sparkContext.setJobGroup(jobId, cmd, interruptOnCancel = true)
              if (runningJobs.containsKey(jobId)) {
                val ts = if (txDate != null && txDate.length > 0) {
                  DateTimeUtils.getMillisFromDateTimeExp(txDate, "yyyy-MM-dd HH:mm:ss")
                } else {
                  System.currentTimeMillis()
                }
                val sql = TxDateUtils.parse(cmd, ts)
                refreshUpdatedDepTables(sql, getDefaultHiveContext())
                resolveQueryTables(sql)
                result = getDefaultHiveContext().sql(sql)
              }
            })
          } catch {
            case t: Throwable =>
              if (jobStatus.status.equals("running")) {
                jobStatus.status = "error"
                if (jobStatus.elapsed.isEmpty) {
                  jobStatus.elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
                }
              }
              throw t
          }
        }
      )

      var elapsed: String = ""
      val (columns: Option[Array[String]], serializableInputStream: SerializableInputStream) = if (result != null) {
        tmpPath = TMP_DIR + "/dora-query-" + System.currentTimeMillis() + "-" + System.nanoTime()
        result.write.json(tmpPath)
        inputStream = HdfsUtils.readFilesContentFromDir(tmpPath)
        elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
        jobStatus.elapsed = elapsed
        (Option(result.columns), new SerializableInputStream(inputStream))
      } else {
        elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
        jobStatus.elapsed = elapsed
        writer.write(jobStatus.elapsed)
        (None, new SerializableInputStream(buffer.getInputStream))
      }
      runningJobs.synchronized {
        if (runningJobs.containsKey(jobId)) {
          jobStatus.status = "success"
          if (elapsed.isEmpty) {
            elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
            jobStatus.elapsed = elapsed
          }
          LoggerFactory.getLogger(getClass) info ("Finished processing query '" + jobId + "'")
          runningJobs.remove(jobId)
        }
      }
      (columns, serializableInputStream)
    } catch {
      case t: TimeoutException =>
        jobStatus.elapsed = DateTimeUtils.getTimeDiffExp(start, start + queryTimeoutMillis)
        jobStatus.status = "timeout"
        LoggerFactory.getLogger(getClass).error("Error", t)
        throw new RuntimeException("Query execution time exceeded limit")
      case t: Throwable =>
        jobStatus.elapsed = DateTimeUtils.getTimeDiffExp(start, System.currentTimeMillis())
        jobStatus.status = "error"
        LoggerFactory.getLogger(getClass).error("Error", t)
        throw t
    } finally {
      writer.flush()
      writer.close()
      try {
        if (tmpPath != null) {
          HdfsUtils.delete(tmpPath)
        }
      } catch {
        case ignored: Throwable =>
      }
    }
  }

  override def stop(user: String, jobId: String): Unit = {
    runningJobs.synchronized {
      runningJobs.remove(jobId)
      cancelledJobs.put(jobId, jobId)
      LoggerFactory.getLogger(getClass).info("'" + user + "' marked job '" + jobId + "' as cancelled")
      val jobStatus = jobHistory.getIfPresent(jobId)
      if (jobStatus != null && jobStatus.status.equals("running")) {
        jobStatus.status = "cancelled by " + user
        if (jobStatus.elapsed.isEmpty) {
          jobStatus.elapsed = DateTimeUtils.getTimeDiffExp(jobStatus.startMillis.toLong, System.currentTimeMillis())
        }
      }
    }
  }

  override def meta(user: String, scope: String, target: String): Array[String] = {
    var result: Array[String] = null
    scope match {
      case "table" =>
        result = TableInfoCache.keySet().toList.sorted.toArray
      case "field" =>
        if (target.contains(".")) {
          val (schema, table) = target.split('.') match {
            case Array(s, t) => (s, t)
          }
          resolveQueryTables("select 1 from " + table)
          result = TableMetaUtils.getFields(getDefaultHiveContext(), table)
        }
    }
    if (result == null) {
      throw new RuntimeException("Unable to retrieve metadata")
    }
    result
  }

  override def status(user: String): List[QueryStatus] = {
    jobHistory.asMap().values().toArray.map(
      x => x.asInstanceOf[QueryStatus]).sortBy(_.startMillis)(Ordering[String].reverse).toList
  }

  override def listTemplates(user: String): Map[String, String] = {
    JsonUtils.fromJson[Map[String, String]](MetaDao.get(MetaTable.QUERY_TEMPLATE, user).getOrElse("{}"))
  }

  override def setTemplate(user: String, name: String, sql: String): Unit = {
    val old = JsonUtils.fromJson[Map[String, String]](MetaDao.get(MetaTable.QUERY_TEMPLATE, user).getOrElse("{}"))
    val updated = old.filter(x => (!x._1.equals(name))) + ((name, sql))
    MetaDao.set(MetaTable.QUERY_TEMPLATE, user, JsonUtils.toJson(updated))
  }

  override def deleteTemplate(user: String, name: String): Unit = {
    val old = JsonUtils.fromJson[Map[String, String]](MetaDao.get(MetaTable.QUERY_TEMPLATE, user).getOrElse("{}"))
    val updated = old.toArray.filter(x => (!x._1.equals(name))).toMap
    MetaDao.set(MetaTable.QUERY_TEMPLATE, user, JsonUtils.toJson(updated))
  }

  private def resolveQueryTables(sql: String): Unit = {
    val t = new Thread(new Runnable() {
      override def run(): Unit = {
        LOG.info("Start resolving query tables")
        val tables = SqlUtils.extractTablesFromQuery(sql)
        tables.foreach(name => {
          GlobalSync.get(name).synchronized{
            if (!getDefaultHiveContext().sparkSession.catalog.tableExists(name)) {
              LOG.info("Detected unresolved table: " + name)
              val meta = TableInfoCache.retrieveUpdatedTableInfo().get(name).getOrElse(null)
              if (meta != null) {
                LOG.info("Resolving table: " + name + " => " + meta.path)
                getDefaultHiveContext().read.parquet(meta.path).createOrReplaceTempView(name)
                LOG.info("Resolved table: " + name)
              } else {
                LOG.info("Unable to resolve table: " + name)
              }
            } else {
              LOG.info("Resolved table: " + name)
            }
          }
        })
        LOG.info("Finished resolving query tables")
      }
    })
    t.start()
    t.join()
  }


  private val cancelledJobs = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build[String, String]()
  private val cancelJobsThread = new Thread(new Runnable() {
    override def run() = {
      while (true) {
        Thread.sleep(1000)
        try {
          val activeJobIds = getSparkContext().statusTracker.getActiveJobIds()
          cancelledJobs.asMap().keySet().toArray.map(x => x.asInstanceOf[String]).foreach(groupId => {
            var count = 0
            val jobIds = getSparkContext().statusTracker.getJobIdsForGroup(groupId)
            jobIds.foreach(id => {
              if (activeJobIds.contains(id)) {
                getSparkContext().cancelJob(id)
                val jobInfo = getSparkContext().statusTracker.getJobInfo(id).getOrElse(null)
                if (jobInfo.status() != JobExecutionStatus.RUNNING) {
                  count += 1
                }
              }
            })
            if (jobIds.length > 0 && count > 0) {
              cancelledJobs.invalidate(groupId)
            }
          })
        } catch {
          case _: Throwable =>
        }
      }
    }
  })
  cancelJobsThread.setDaemon(true)
  cancelJobsThread.start()
}