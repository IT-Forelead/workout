#!/bin/bash

sbt docker:publishLocal
cd app/ || exit
source run.sh