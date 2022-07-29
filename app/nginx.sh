#!/bin/bash
mkdir /srv/nginx_conf
mkdir /srv/web

rm -rf /srv/nginx_conf/trim.uz.conf
tee -a /srv/nginx_conf/trim.uz.conf > /dev/null <<EOT
server {
  listen 80 default_server;
	listen [::]:80 default_server;

 	server_name trim.uz www.trim.uz;
 	location / {
    proxy_pass http://127.0.0.1:80;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
    proxy_set_header Host $host;
  }
  location ~ ^/api/*$ {
    proxy_pass http://127.0.0.1:9000;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
    proxy_set_header Host $host;
  }
}
EOT