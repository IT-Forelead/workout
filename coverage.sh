#!/bin/bash

sbt -mem 3000 clean reload coverage "project root" "runTests" "runItTests" coverageAggregate
