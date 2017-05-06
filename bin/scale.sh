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
    entities=$(expr $entities + 1)
    echo $entities
    sleep 1
  done
}

function lamp {
  wait_until_num_entities 8
}

subcommand=$1
shift

case $subcommand in
  lamp) lamp "$@" ;;
  *) usage_exit ;;
esac
