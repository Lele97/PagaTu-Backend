#!/usr/bin/make

build-auth:
	bash buildone.sh auth
build-coffee:
	bash buildone.sh coffee
build-eureka:
	bash buildone.sh eureka
build-gateway:
	bash buildone.sh gateway
build-mail:
	bash buildone.sh mail
build-all:
	bash buildone.sh mail