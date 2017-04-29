#!/bin/sh

echo Using AKKA_HOSTNAME: ${AKKA_HOSTNAME:=`curl http://169.254.169.254/latest/meta-data/local-ipv4`}
export AKKA_HOSTNAME

echo Using SEED_ADDR: ${SEED_ADDR:=akka.tcp://cluster@127.0.0.1:2551}
export SEED_ADDR

echo Using MAX_HEAP_SIZE: ${MAX_HEAP_SIZE:=200m}
export MAX_HEAP_SIZE

java \
  -Xmx$MAX_HEAP_SIZE \
  -Dakka.cluster.seed-nodes.0=$SEED_ADDR \
  -Dconfig.resource=$CONFIG \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=7900 \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -jar cat-cluster.jar
