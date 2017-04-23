#!/bin/sh

echo Using AKKA_HOSTNAME: ${AKKA_HOSTNAME:=`curl http://169.254.169.254/latest/meta-data/local-ipv4`}
export AKKA_HOSTNAME

echo Using SEED_ADDR: ${SEED_ADDR:=akka.tcp://cluster@127.0.0.1:2551}
export SEED_ADDR

java \
  -Dakka.cluster.seed-nodes.0=$SEED_ADDR \
  -Dconfig.resource=$CONFIG \
  -jar cat-cluster.jar
