#!/bin/bash

cd `dirname "$0"`;
BUILD_PATH=`pwd`;
BUILD_OUTPUT_PATH="$BUILD_PATH/dist/docker"

rm -Rf $BUILD_OUTPUT_PATH
mkdir -p $BUILD_OUTPUT_PATH;

# build dora

IMG_NAME="dora"
FILE_PATH=$BUILD_OUTPUT_PATH/$IMG_NAME.img
cd $BUILD_PATH
docker rmi $IMG_NAME 2> /dev/null | true
docker build -t $IMG_NAME .
echo "[dora] exporting image ... "
docker save $IMG_NAME | gzip > $FILE_PATH
echo "[dora] saved img to $FILE_PATH"
