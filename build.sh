#!/bin/sh
gradle clean bootJar --info
docker build -t arsenal/arsenal-builder --build-arg 'sourceFile=./build/libs/arsenal*.jar' -f ./devops/jenkins/Dockerfile .

docker tag arsenal/arsenal-builder ktis-bastion01.container.ipc.kt.com:5000/arsenal/arsenal-builder
docker push ktis-bastion01.container.ipc.kt.com:5000/arsenal/arsenal-builder
