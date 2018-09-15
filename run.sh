#!/bin/bash

IMG_NAME="dora"

gunzip -c dist/docker/$IMG_NAME.img | docker load

docker stop $IMG_NAME
docker rm $IMG_NAME
docker run -d \
--name $IMG_NAME \
--network="host" \
-h $IMG_NAME-`hostname` \
-v /etc/hosts:/etc/hosts:ro \
-v /etc/timezone:/etc/timezone:ro \
-v /etc/localtime:/etc/localtime:ro \
-v /opt/hadoop:/opt/hadoop:ro \
$IMG_NAME

echo "[$project_name] login into container ... "

docker exec -ti $IMG_NAME bash
