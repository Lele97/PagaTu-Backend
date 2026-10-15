#!/usr/bin/make

build-auth:
	bash script/buildone.sh auth
build-coffee:
	bash script/buildone.sh coffee
build-eureka:
	bash script/buildone.sh eureka
build-gateway:
	bash script/buildone.sh gateway
build-mail:
	bash script/buildone.sh mail
build-all:
	bash script/buildone.sh all