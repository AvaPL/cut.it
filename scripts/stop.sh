#!/bin/bash

cd ../

# Process script arguments
while [[ "$#" -gt 0 ]]; do
  case $1 in
  # TODO: Extract cases to functions
  cut.it | infra) profile=$1 ;; # TODO: Extract profiles to a variable
  *)
    echo "Unknown parameter passed: $1"
    exit 1
    ;;
  esac
  shift
done

# Check specified profile
if [ -z "$profile" ]; then
  echo -e "No profile specified, use one of: [cut.it, infra]"
  exit 1
fi

cd docker/"$profile" || exit
docker-compose down
cd ../../scripts || exit