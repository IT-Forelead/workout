#!/bin/bash
mkdir /srv/nginx_conf
mkdir /srv/web

rm -rf /srv/nginx_conf/trim.uz.conf
tee -a /srv/nginx_conf/trim.uz.conf > /dev/null <<EOT
server {
  listen 80 default_server;
	listen [::]:80 default_server;

 	server_name trim.uz www.trim.uz;
}
EOT

docker-compose stop nginx-server
docker-compose up -d nginx-server