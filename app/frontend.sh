#!/bin/bash
source ../env.sh
docker-compose stop frontend
docker-compose up -d frontend