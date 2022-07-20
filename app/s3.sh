#!/bin/bash
source ../env.sh
docker-compose stop localstack
docker-compose up -d localstack