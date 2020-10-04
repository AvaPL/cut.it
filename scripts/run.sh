#!/bin/bash

# TODO: Add argument for selecting cut.it/infra docker-compose files

cd ../
while [[ "$#" -gt 0 ]]; do
    case $1 in
        # TODO: Update with cut-link project
        -i|--images) echo "Building Docker images ..."; sbt "; project basic-graphql; docker:publishLocal"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done
cd docker || exit
docker-compose up -d
cd ../scripts || exit