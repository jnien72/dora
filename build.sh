#!/bin/bash

build_env="$1"

PROJECT_NAME="dora";
cd `dirname "$0"`;
BUILD_PATH=`pwd`;
OUTPUT_PATH="${BUILD_PATH}/dist"
OUTPUT_RUNTIME_PATH="${OUTPUT_PATH}/${PROJECT_NAME}"

cd ${BUILD_PATH}
rm -rf ${OUTPUT_PATH}
sbt clean assembly

BUILD_EXIT_CODE="$?";
if [ "$BUILD_EXIT_CODE" != "0" ]; then
    echo "Build Finished with error(s)"
    exit 1;
fi;

mkdir -p ${OUTPUT_RUNTIME_PATH}

mkdir ${OUTPUT_RUNTIME_PATH}/etc

for f in ${BUILD_PATH}/conf/*; do
	cp $f ${OUTPUT_RUNTIME_PATH}/etc >> /dev/null 2>&1
done

if [ "$build_env" == "prod" ]; then
    for f in ${BUILD_PATH}/conf-prod/*; do
        cp $f ${OUTPUT_RUNTIME_PATH}/etc >> /dev/null 2>&1
    done
fi

cp -r ${BUILD_PATH}/sbin ${OUTPUT_RUNTIME_PATH}/sbin
chmod 755 ${OUTPUT_RUNTIME_PATH}/sbin/*

cp -r ${BUILD_PATH}/bin ${OUTPUT_RUNTIME_PATH}/bin
chmod 755 ${OUTPUT_RUNTIME_PATH}/bin/*

mkdir -p ${OUTPUT_RUNTIME_PATH}/lib
cp target/scala-*/*.jar ${OUTPUT_RUNTIME_PATH}/lib

mkdir -p ${OUTPUT_RUNTIME_PATH}/pid

echo
echo "Build finished successfully => ${OUTPUT_PATH}"
