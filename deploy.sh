#! /bin/sh

mvn clean package

docker ps -a | awk '{ print $1,$2 }' | grep magnoabreu/cryptobrute:1.0 | awk '{print $1 }' | xargs -I {} docker rm -f {}
docker rmi magnoabreu/cryptobrute:1.0
docker build --tag=magnoabreu/cryptobrute:1.0 --rm=true .

docker run --name cryptobrute \
-v /etc/localtime:/etc/localtime:ro \
-v /srv/cryptobrute:/brute \
-p 8087:8080 \
-d magnoabreu/cryptobrute:1.0


