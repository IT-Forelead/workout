# S3 commands
## List buckets
`aws s3api list-buckets --endpoint-url http://localhost:4566`
## List files in the bucket folder
`aws s3 ls s3://BUCKET_NAME/FOLDER_NAME/ --endpoint-url http://localhost:4566`
## Copy file into bucket
`aws s3 cp FILE_NAME s3://BUCKET_NAME/FOLDER/ --endpoint-url http://localhost:4566`
## Copy file from bucket to current folder
`aws s3 cp s3://BUCKET_NAME/FOLDER/FILE_NAME ./ --endpoint-url http://localhost:4566`