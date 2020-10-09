#!/bin/bash

source common.sh
projects=("cut-link")

cd ../

function build_images() {
  echo "[info] Building Docker images ..."
  sbt "$(printf "; project %s; docker:publishLocal" "${projects[@]}")"
}

# Process script arguments
while [[ "$#" -gt 0 ]]; do
  case $1 in
  -i | --images) build_images ;;
  cut.it | infra) profile=$1 ;;
  *) unknown_parameter "$1" ;;
  esac
  shift
done

# Start containers
if [ -z "$profile" ]; then
  echo -e "[\e[1;33mwarning\e[0m] No profile specified. If you want to also start the app use one of: [cut.it, infra]"
else
  cd docker/"$profile" || exit
  docker-compose up -d
  cd ../../scripts || exit
fi
