#!/bin/bash

cd `dirname "$0"`;
BUILD_PATH=`pwd`;
BUILD_OUTPUT_PATH="$BUILD_PATH/dist/docker"

rm -Rf $BUILD_OUTPUT_PATH
mkdir -p $BUILD_OUTPUT_PATH;

# build web

IMG_NAME="dora-web"
FILE_PATH=$BUILD_OUTPUT_PATH/$IMG_NAME.img
cd $BUILD_PATH
sed 's/\[START_CMD\]/\/opt\/dora\/sbin\/start-web.sh/g' \
 docker/dockerfile-template > dist/Dockerfile
cd dist
docker rmi $IMG_NAME 2> /dev/null | true
docker build -t $IMG_NAME .
echo "[ecs server] exporting image ... "
docker save $IMG_NAME | gzip > $FILE_PATH
echo "[ecs server] saved img to $FILE_PATH"

#build agent

IMG_NAME="dora-agent"
FILE_PATH=$BUILD_OUTPUT_PATH/$IMG_NAME.img
cd $BUILD_PATH
sed 's/\[START_CMD\]/\/opt\/dora\/sbin\/start-agent.sh/g' \
 docker/dockerfile-template > dist/Dockerfile
cd dist
docker rmi $IMG_NAME 2> /dev/null | true
docker build -t $IMG_NAME .
echo "[ecs loader] exporting image ... "
docker save $IMG_NAME | gzip > $FILE_PATH
echo "[ecs loader] saved img to $FILE_PATH"