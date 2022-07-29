#!/bin/bash
mkdir /srv/nginx_conf
mkdir /srv/web

rm -rf /srv/nginx_conf/trim.uz.conf
tee -a /srv/nginx_conf/trim.uz.conf > /dev/null <<EOT
server {
  listen 80 default_server;
	listen [::]:80 default_server;

 	server_name trim.uz www.trim.uz;
 	  location ~ ^/api/*$ {
      proxy_pass http://localhost:9000;
      proxy_http_version 1.1;
      proxy_set_header X-Real-IP  \$remote_addr;
      proxy_set_header X-Forwarded-For \$remote_addr;
      proxy_set_header Host \$host;

      # secure websocket support
      proxy_set_header Upgrade \$http_upgrade;
      proxy_set_header Connection "upgrade";
    }
}
EOT

docker-compose stop nginx-server
docker-compose up -d nginx-server