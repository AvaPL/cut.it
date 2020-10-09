#!/bin/bash

source common.sh

cd ../

# Process script arguments
while [[ "$#" -gt 0 ]]; do
  case $1 in
  cut.it | infra) profile=$1 ;;
  *) unknown_parameter "$1" ;;
  esac
  shift
done

# Stop containers
if [ -z "$profile" ]; then
  echo -e "[\e[0;31merror\e[0m] No profile specified. Use one of: [cut.it, infra]"
else
  cd docker/"$profile" || exit
  docker-compose down
  cd ../../scripts || exit
fi
