#!/bin/bash

cd `dirname "$0"`;
cd ..

export APP_HOME=`pwd`;

function killApp(){
    appPattern=$1
    pid=`jps -lm | grep "$appPattern" | awk '{print $1}'`;
    if [ ! "$pid" == "" ] ; then
        echo "Terminating $appPattern..."
        kill -9 $pid;
        echo "[$pid] $appPattern stopped"
    else
        echo "Error: $appPattern is not running";
    fi;
}

killApp com.eds.dora.cluster;
sleep 1;