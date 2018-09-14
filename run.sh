#!/bin/bash

cd `dirname "$0"`;

project_name="dora"

echo "[$project_name] cleaning existing image & container... "
docker stop $project_name 2> /dev/null | true
docker rm $project_name 2> /dev/null | true
docker rmi $project_name 2> /dev/null | true

echo "[$project_name] building docker image ... "
docker build -t $project_name .

echo "[$project_name] starting container ..."
docker run -d \
--name $project_name \
--network="host" \
-h `hostname` \
$project_name

echo "[$project_name] login into container ... "

docker exec -ti $project_name bash
