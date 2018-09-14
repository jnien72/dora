#!/bin/bash

cd `dirname "$0"`;
cd ..

export APP_HOME=`pwd`;

if [ ! -d "$APP_HOME/logs" ]; then
	mkdir $APP_HOME/logs
fi

####
#   These jars can't be merged, they have are signed somehow, need to be alone as it is :(
###

if [ ! -f "./lib/datanucleus-api-jdo-3.2.6.jar" ]; then
    wget http://central.maven.org/maven2/org/datanucleus/datanucleus-api-jdo/3.2.6/datanucleus-api-jdo-3.2.6.jar > /dev/null 2>&1
    mv datanucleus-api-jdo-3.2.6.jar lib
fi
if [ ! -f "./lib/datanucleus-core-3.2.10.jar" ]; then
    wget http://central.maven.org/maven2/org/datanucleus/datanucleus-core/3.2.10/datanucleus-core-3.2.10.jar > /dev/null 2>&1
    mv datanucleus-core-3.2.10.jar lib
fi
if [ ! -f "./lib/datanucleus-rdbms-3.2.9.jar" ]; then
    wget http://central.maven.org/maven2/org/datanucleus/datanucleus-rdbms/3.2.9/datanucleus-rdbms-3.2.9.jar > /dev/null 2>&1
    mv datanucleus-rdbms-3.2.9.jar lib
fi

CLASSPATH="";

for jar_file in $APP_HOME/lib/*.jar; do
	CLASSPATH="$CLASSPATH:$jar_file";
done;

CLASSPATH="etc:$CLASSPATH";

JVM_OPTS="$JVM_OPTS -Dfile.encoding=UTF-8";
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
JVM_OPTS="$JVM_OPTS -Xmx64m"
JVM_OPTS="$JVM_OPTS -Xms64m"

pid=pid/dora-agent.pid
log=logs/dora-agent.log

if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
fi

echo starting dora agent, sending stdout/stderr to $log
nohup java -classpath $CLASSPATH $JVM_OPTS com.eds.dora.cluster.DoraAgent "$@" > "$log" 2>&1 < /dev/null &
echo $! > $pid
sleep 1; head "$log"