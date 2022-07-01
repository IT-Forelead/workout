#!/bin/bash
docker-compose down
docker-compose up -d
export AWS_ACCESS_KEY_ID=dummy
export AWS_SECRET_ACCESS_KEY=dummy