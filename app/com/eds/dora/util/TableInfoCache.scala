package com.eds.dora.util
import java.util.concurrent.ConcurrentHashMap
import com.eds.dora.cluster.util.InstanceUtils
import org.I0Itec.zkclient.IZkDataListener
import org.slf4j.LoggerFactory

import scala.collection._
import scala.collection.convert.decorateAsScala._


object TableInfoCache {

  @volatile
  private var currentTableInfoMap = Map[String, TableMeta]()
  private var sparkContainer: SparkContainer = null

  def notifyUpdate(): Unit = {
    val zkPath = ZkConstants.ZK_NAMESPACE_HOME + "/" + InstanceUtils.getNamespace() + "/metadata"
    ZkClient.get().writeData(zkPath, System.currentTimeMillis() + "-" + System.nanoTime())
  }

  def setup(sparkContainer: SparkContainer): Unit = {
    this.sparkContainer = sparkContainer
    sparkContainer.getDefaultHiveContext()
    setup()
  }

  def setup(): Unit = {
    val zkPath = ZkConstants.ZK_NAMESPACE_HOME + "/" + InstanceUtils.getNamespace() + "/metadata"
    ZkClient.get().subscribeDataChanges(zkPath,
      new IZkDataListener {
        override def handleDataChange(dataPath: String, data: scala.Any) = {
          new Thread(refreshTableInfoRunnable).start()
        }

        override def handleDataDeleted(dataPath: String) = ???
      }
    )
    refreshTableInfoRunnable.run()
  }

  private val refreshTableInfoRunnable = new Runnable() {
    override def run(): Unit = {
        val retrievedTableInfo = retrieveUpdatedTableInfo()
        val updatedTableInfo = new ConcurrentHashMap[String, TableMeta]().asScala

        val addedTables = retrievedTableInfo.keySet.filter(name => (currentTableInfoMap.getOrElse(name, null)) == null).toList.sorted
        val removedTables = currentTableInfoMap.keySet.filter(name => (retrievedTableInfo.getOrElse(name, null)) == null).toList.sorted
        val intersectTables = currentTableInfoMap.keySet.intersect(retrievedTableInfo.keySet).toList.sorted

        LoggerFactory.getLogger(getClass).info("Start synchronizing table metadata...")

        addedTables.foreach(name => {
          try {
            updatedTableInfo.put(name, retrievedTableInfo.get(name).get)
          } catch {
            case _: Throwable =>
          }
        })

        removedTables.foreach(name => {
          try {
            if (sparkContainer != null) {
              GlobalSync.get(name).synchronized {
                LoggerFactory.getLogger(getClass).info("Removing table => " + name)
                if (sparkContainer.getDefaultHiveContext().sparkSession.catalog.tableExists(name)) {
                  sparkContainer.getDefaultHiveContext().dropTempTable(name)
                }
              }
            }
          } catch {
            case _: Throwable => {
              updatedTableInfo.put(name, retrievedTableInfo.get(name).get) //fail to remove, keep
            }
          }
        })

        intersectTables.foreach(name => {
          try {
            if (sparkContainer != null) {
              val before = currentTableInfoMap.get(name).getOrElse(null)
              val after = retrievedTableInfo.get(name).getOrElse(null)
              val shouldExpire = !(before != null && after != null
                && before.lastUpdate.getOrElse(-1) == after.lastUpdate.getOrElse(-1))
              GlobalSync.get(name).synchronized {
                if (shouldExpire && sparkContainer.getDefaultHiveContext().sparkSession.catalog.tableExists(name)) {
                  LoggerFactory.getLogger(getClass).info("Updating table => " + name)
                  sparkContainer.getDefaultHiveContext().dropTempTable(name)
                }
              }
            }
            updatedTableInfo.put(name, retrievedTableInfo.get(name).get)
          } catch {
            case _: Throwable =>
          }
        })
        currentTableInfoMap = updatedTableInfo.toMap
        LoggerFactory.getLogger(getClass).info("Finished synchronizing table metadata...")
    }
  }

  def get(): Map[String, TableMeta] = {
    currentTableInfoMap.toMap
  }

  def keySet(): immutable.Set[String] = {
    currentTableInfoMap.keySet.toSet
  }

  def retrieveUpdatedTableInfo(): Map[String, TableMeta] = {
    InstanceUtils.getNamespaceInfo().predefinedTables.map(entry => {
      val name = entry._1
      val path = entry._2
      (name, TableMeta(name, path, None, None, None, None, None))
    })
  }
}