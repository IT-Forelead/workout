#!/bin/bash

source env.sh
sbt -mem 3000 clean reload coverage "project root" "runTests" "runItTests" coverageAggregate
