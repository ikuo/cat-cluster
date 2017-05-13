A sandbox of journal sweeper by Akka Persistence Query.

## Building

At first, publish [safety-data/akka-persistence-redis](https://github.com/safety-data/akka-persistence-redis) locally.

Then `sbt assembly`.

## Running


1) sbt:

```
sbt> re-start
```

2) Single container:

```
sudo docker run -it --env "CONFIG=/seed.conf" --env "AKKA_HOSTNAME=127.0.0.1" ikuo/cat-cluster
```

3) Local cluster:

```
docker build -t $USER/cat-cluster . && ENV=dev docker-compose up
```

4) Cluster on docker containers

```
docker-compose up
```
