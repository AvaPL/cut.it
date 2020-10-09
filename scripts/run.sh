#!/bin/bash

cd ../

# Process script arguments
while [[ "$#" -gt 0 ]]; do
  case $1 in
  # TODO: Extract cases to functions
  -i | --images)
    echo "Building Docker images ..."
    sbt "; project cut-link; docker:publishLocal" # TODO: Extract project names to a variable
    ;;
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

# Start containers
cd docker/"$profile" || exit
docker-compose up -d
cd ../../scripts || exit
