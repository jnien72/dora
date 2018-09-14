/**
  * johnny.n comments:
  * This class overrides actual SQLExecution class in original spark,
  * An workaround for issue related to (execution id already set) is applied
  * otherwise dora's spark containers might crash easily
  */

package org.apache.spark.sql.execution

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.ui.{SparkListenerSQLExecutionEnd, SparkListenerSQLExecutionStart}

object SQLExecution {

  val EXECUTION_ID_KEY = "spark.sql.execution.id"

  private val _nextExecutionId = new AtomicLong(0)

  private def nextExecutionId: Long = _nextExecutionId.getAndIncrement

  private val executionIdToQueryExecution = new ConcurrentHashMap[Long, QueryExecution]()

  def getQueryExecution(executionId: Long): QueryExecution = {
    executionIdToQueryExecution.get(executionId)
  }

  /**
    * Wrap an action that will execute "queryExecution" to track all Spark jobs in the body so that
    * we can connect them with an execution.
    */
  def withNewExecutionId[T](
                             sparkSession: SparkSession,
                             queryExecution: QueryExecution)(body: => T): T = {
    val sc = sparkSession.sparkContext
    val oldExecutionId = sc.getLocalProperty(EXECUTION_ID_KEY)
    val executionId: Long = if (oldExecutionId == null) {
      nextExecutionId
    } else {
      oldExecutionId.toLong
    }


    sc.setLocalProperty(EXECUTION_ID_KEY, executionId.toString)
    executionIdToQueryExecution.put(executionId, queryExecution)
    val r = try {
      val callSite = sparkSession.sparkContext.getCallSite()
      sparkSession.sparkContext.listenerBus.post(SparkListenerSQLExecutionStart(
        executionId, callSite.shortForm, callSite.longForm, queryExecution.toString,
        SparkPlanInfo.fromSparkPlan(queryExecution.executedPlan), System.currentTimeMillis()))
      try {
        body
      } finally {
        sparkSession.sparkContext.listenerBus.post(SparkListenerSQLExecutionEnd(
          executionId, System.currentTimeMillis()))
      }
    } finally {
      executionIdToQueryExecution.remove(executionId)
      sc.setLocalProperty(EXECUTION_ID_KEY, null)
    }
    r
  }

  /**
    * Wrap an action with a known executionId. When running a different action in a different
    * thread from the original one, this method can be used to connect the Spark jobs in this action
    * with the known executionId, e.g., `BroadcastHashJoin.broadcastFuture`.
    */
  def withExecutionId[T](sc: SparkContext, executionId: String)(body: => T): T = {
    val oldExecutionId = sc.getLocalProperty(SQLExecution.EXECUTION_ID_KEY)
    try {
      sc.setLocalProperty(SQLExecution.EXECUTION_ID_KEY, executionId)
      body
    } finally {
      sc.setLocalProperty(SQLExecution.EXECUTION_ID_KEY, oldExecutionId)
    }
  }
}