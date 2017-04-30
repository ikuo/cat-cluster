#!/bin/bash

set -eu

function usage_exit {
  cat <<EOS
Usage:
  $0 pull-dc
  $0 package
  $0 pull-ec2 <instance-id>
EOS
  exit 1
}

if [ $# -eq 0 ]; then usage_exit; fi

function pull_dc {
  target=./log
  sudo docker ps --format '{{.Names}}' \
    | grep '^catcluster_cat' \
    | xargs -n 1 -I {} sudo docker cp {}:/opt/cat-cluster/log/profile.log $target/{}.profile.log
}

function package {
  base=./log
  target=$base/logs.gz
  tar zvcf $target --exclude "*.gz" --exclude ".gitkeep" --exclude "profile.log" $base
  echo Created $target
}

subcommand=$1
shift

case $subcommand in
  pull-dc) pull_dc "$@" ;;
  package) package "$@" ;;
  *) usage_exit ;;
esac
