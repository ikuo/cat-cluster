#!/bin/bash

set -eu

function usage_exit {
  cat <<EOS
Usage:
  $0 lamp
EOS
  exit 1
}

if [ $# -eq 0 ]; then usage_exit; fi

function wait_until_num_entities {
  target=$1
  entities=0
  while [ $entities -lt $target ]; do
    entities=$(ENV=small docker-compose exec --index=1 cat tail -1 log/profile.log|awk -F"\t" '{print $4}')
    echo $entities
    sleep 2
  done
}

function lamp {
  wait_until_num_entities 50000
  ENV=small docker-compose scale cat=5
  wait_until_num_entities 100000
  ENV=small docker-compose scale cat=10
  wait_until_num_entities 150000
  ENV=small docker-compose scale cat=15
  wait_until_num_entities 200000
  ENV=small docker-compose scale sensor=2
  wait_until_num_entities 400000
  ENV=small docker-compose scale sensor=3
  wait_until_num_entities 600000
  ENV=small docker-compose scale sensor=4
  wait_until_num_entities 1000000
  ENV=small docker-compose scale sensor=5
  wait_until_num_entities 1500000
  ENV=small docker-compose scale sensor=6
}

subcommand=$1
shift

case $subcommand in
  lamp) lamp "$@" ;;
  *) usage_exit ;;
esac
