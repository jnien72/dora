package com.eds.dora.util


object EnvConstants extends Enumeration {
  type Key = Value

  val SERVICE_PORT = Value("http.port")

  val HADOOP_CONF_DIR = Value("hadoop.conf.dir")

  val ZK_QUORUM = Value("zk.quorum")
  val ZK_CONNECTION_TIMEOUT = Value("zk.conn.timeout")
  val ZK_SESSION_TIMEOUT = Value("zk.session.timeout")

  val KAFKA_ZK_ROOT=Value("kafka.zk.root")
  val KAFKA_PARTITIONS=Value("kafka.default.partitions")
  val KAFKA_REPLICATIONS=Value("kafka.default.replications")

  val META_DB_DIR = Value("meta.db.dir")

  val TOPOLOGY_EDITOR = Value("topology.editor")

  val AGENT_HEAP_CAPACITY = Value("agent.heap.capacity")
  val AGENT_INSTANCE_SERVICE_PORT_RANGE_START = Value("agent.instance.service.port.range.start")
  val AGENT_INSTANCE_SERVICE_PORT_RANGE_STOP = Value("agent.instance.service.port.range.stop")
  val AGENT_INSTANCE_SPARK_UI_PORT_RANGE_START = Value("agent.instance.spark.ui.port.range.start")
  val AGENT_INSTANCE_SPARK_UI_PORT_RANGE_STOP = Value("agent.instance.spark.ui.port.range.stop")

  val QUERY_TIMEOUT_MILLIS = Value("query.timeout.millis")
  val QUERY_WRITE_PERMISSION = Value("query.write.permission")
  val QUERY_RESULT_PAGE_SIZE = Value("query.result.page.size")

  val DS_IMPORT_MAX_CONCURRENCY = Value("ds.import.max.concurrency")
  val DS_PARQUET_WRITER_STATUS_INTERVAL_SECONDS = Value("ds.parquet.writer.status.interval.seconds")
  val DS_SHELL_RUN_AS_USER = Value("ds.shell.run-as.username")
  val DS_SHELL_RUN_AS_IDENTITY_FILE = Value("ds.shell.run-as.identity.file")
  val DS_SHELL_HOST = Value("ds.shell.host")
  val DS_DATA_DIR = Value("ds.data.dir")

  val ETL_IMPORT_MAX_CONCURRENCY = Value("etl.import.max.concurrency")
  val ETL_SCHEDULE_ENABLED = Value("etl.scheduler.enabled")
  val ETL_SCHEDULE_MISSING = Value("etl.scheduler.reschedule.missing")
  val ETL_DATA_DIR = Value("etl.data.dir")
  val ETL_REPORT_DIR = Value("etl.report.dir")
  val ETL_DEPENDENCY_CHECK = Value("etl.dependency.check")
  val ETL_EXPORTABLE_VERSIONS = Value("etl.exportable.versions")

  val WEB_DELAY_MILLIS_MIN = Value("web.delay.millis.min")
  val WEB_DELAY_MILLIS_MAX = Value("web.delay.millis.max")

  val TMP_DIR = Value("tmp.dir")

  val SLACK_NOTIFICATION_HOOK_URL = Value("slack.notification.hook-url")
  val SLACK_NOTIFICATION_CHANNEL = Value("slack.notification.channel")
  val SLACK_NOTIFICATION_USER = Value("slack.notification.user")

}