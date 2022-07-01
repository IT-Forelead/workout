#!/bin/bash
source ../env.sh
docker-compose stop redis
docker-compose up -d redis