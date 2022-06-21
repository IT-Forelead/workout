#!/bin/bash
set -x
awslocal s3 mb s3://s3-bucket # s3_bucket - it's BUCKET_NAME
set +x
