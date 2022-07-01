#!/bin/bash
source env.sh
sbt -mem 3000 "project root" "runItTests"