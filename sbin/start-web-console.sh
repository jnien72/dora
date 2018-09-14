
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
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=20"
JVM_OPTS="$JVM_OPTS -XX:InitiatingHeapOccupancyPercent=35"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDateStamps";
JVM_OPTS="$JVM_OPTS -XX:+PrintHeapAtGC";
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails";
JVM_OPTS="$JVM_OPTS -XX:+PrintTenuringDistribution";
JVM_OPTS="$JVM_OPTS -XX:+AlwaysPreTouch";
JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops";
JVM_OPTS="$JVM_OPTS -Xmx128m"
JVM_OPTS="$JVM_OPTS -Xms128m"

pid=pid/web-console.pid

if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
fi

echo starting server
nohup java -classpath $CLASSPATH $JVM_OPTS com.eds.dora.web.WebConsole "$@" &
echo $! > $pid