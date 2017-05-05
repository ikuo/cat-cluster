#!/bin/bash

set -eu

function usage_exit {
  cat <<EOS
Usage:
  $0 pull [<instance-id>]
  $0 pack
  $0 unpack
EOS
  exit 1
}

if [ $# -eq 0 ]; then usage_exit; fi

function pull {
  if [ $# -eq 0 ]; then
    pull_from_docker_compose
  else
    pull_from_ec2 "$@"
  fi
}

function pull_from_docker_compose {
  target=./log

  for name in profile profile.sweeper; do
    sudo docker ps --format '{{.Names}}' \
      | grep '^catcluster_cat' \
      | xargs -n 1 -I {} sudo docker cp {}:/opt/cat-cluster/log/${name}.log $target/{}.${name}.log
  done
}

function pull_from_ec2 {
  key=$1
  ip=$(ec2 ip $key)
  scp ikuo@$ip:/home/ikuo/work/cat-cluster/log/logs.gz log/
}

function pack {
  base=./log
  target=$base/logs.gz
  tar zvcf $target --exclude "*.gz" --exclude ".gitkeep" --exclude "profile.log" $base
  echo Created $target
}

function unpack {
  base=./log
  target=$base/logs.gz
  tar zxvf $target
}

subcommand=$1
shift

case $subcommand in
  pull) pull "$@" ;;
  pack) pack "$@" ;;
  unpack) unpack "$@" ;;
  *) usage_exit ;;
esac
