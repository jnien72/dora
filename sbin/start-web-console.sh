
#!/bin/bash

cd `dirname "$0"`;
cd ..

export APP_HOME=`pwd`;

CLASSPATH="";

if [ ! -d "$APP_HOME/logs" ]; then
	mkdir $APP_HOME/logs
fi

for jar_file in $APP_HOME/lib/*.jar; do
	CLASSPATH="$CLASSPATH:$jar_file";
done;

CLASSPATH="etc:$CLASSPATH";

JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8";
JVM_OPTS="$JVM_OPTS -verbose:gc";
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=20"
JVM_OPTS="$JVM_OPTS -XX:InitiatingHeapOccupancyPercent=35"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDateStamps";
JVM_OPTS="$JVM_OPTS -XX:+PrintHeapAtGC";
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails";
JVM_OPTS="$JVM_OPTS -XX:+PrintTenuringDistribution";
JVM_OPTS="$JVM_OPTS -XX:+UseGCLogFileRotation";
JVM_OPTS="$JVM_OPTS -XX:NumberOfGCLogFiles=1";
JVM_OPTS="$JVM_OPTS -XX:GCLogFileSize=1M";
JVM_OPTS="$JVM_OPTS -XX:+AlwaysPreTouch";
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops";
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError";
JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=logs/web-server-heap-dump.hprof";
JVM_OPTS="$JVM_OPTS -XX:ErrorFile=logs/web-server-java-error.log";
JVM_OPTS="$JVM_OPTS -Xloggc:logs/web-server-gc.log";
JVM_OPTS="$JVM_OPTS -Xmx256m"
JVM_OPTS="$JVM_OPTS -Xms256m"
JVM_OPTS="$JVM_OPTS -Dhdp.version=2.4.2.0"

pid=pid/web-console.pid
log=logs/web-console.log

if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
fi

echo starting server, sending stdout/stderr to $log
nohup java -classpath $CLASSPATH $JVM_OPTS com.eds.dora.web.WebConsole "$@" > "$log" 2>&1 < /dev/null &
echo $! > $pid
sleep 1; head "$log"