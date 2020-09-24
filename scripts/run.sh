#!/bin/bash

cd ../
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -i|--images) echo "Building Docker images ..."; sbt "; project basic-graphql; docker:publishLocal"; shift ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done
cd docker || exit
docker-compose up -d
cd ../scripts || exit