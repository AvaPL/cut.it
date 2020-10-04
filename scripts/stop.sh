#!/bin/bash

# TODO: Use cut.it/infra docker-compose files

cd ../
cd docker || exit
docker-compose down
cd ../scripts || exit