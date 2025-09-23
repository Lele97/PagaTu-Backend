#!/bin/bash

set -e

SUCCESS_MSG="Successfully built:"
ERROR_MSG="admitted values: 'auth', 'coffee', 'eureka', 'gateway', 'mail' or 'all' to build everything"
REGISTRY="registry.pagatu.app"

if [ $# -lt 1 ]; then echo "At least one param required: $ERROR_MSG"; return 0; fi;

if ! echo "$1" | tr '[:upper:]' '[:lower:]' | grep -Eq '^(auth|coffee|eureka|gateway|mail|all)$'; then
 	echo "$ERROR_MSG"
 	return 0
fi

cd "$(dirname "$0")"

# Load environment variables from .env.buildone
if [ -f .env.buildone ]; then
    export $(grep -v '^#' .env.buildone | xargs)
else
    echo "No env file found. Exiting..."
    exit 0
fi

echo "INFO - You can pass the 'all' argument to build and push all the applications"


#git pull origin develop

export DOCKER_CLI_EXPERIMENTAL=enabled
#docker buildx create --use --name multi-builder
#docker buildx inspect --bootstrap

docker login "$REGISTRY" -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"
for var in "$@"
do
    if [ "$var" = "auth" ] || [ "$var" = "all" ]
	  then
	    cd auth
	    mvn clean install -DskipTests
	    docker buildx build --platform linux/amd64,linux/arm64 -t "$REGISTRY"/pagatu-auth:latest --push .
	    SUCCESS_MSG="$SUCCESS_MSG auth "
	    cd ..
    fi

    if [ "$var" = "coffee" ] || [ "$var" = "all" ]
	  then
      cd coffee
      mvn clean install -DskipTests
	    docker buildx build --platform linux/amd64,linux/arm64 -t "$REGISTRY"/pagatu-coffee:latest --push .
	    SUCCESS_MSG="$SUCCESS_MSG coffee "
	    cd ..
    fi

    if [ "$var" = "eureka" ] || [ "$var" = "all" ]
	  then
      cd eureka-server
      mvn clean install -DskipTests
	    docker buildx build --platform linux/amd64,linux/arm64 -t "$REGISTRY"/pagatu-eureka:latest --push .
	    SUCCESS_MSG="$SUCCESS_MSG eureka "
	    cd ..
    fi

    if [ "$var" = "gateway" ] || [ "$var" = "all" ]
	  then
	    cd gateway-service
	    mvn clean install -DskipTests
	    docker buildx build --platform linux/amd64,linux/arm64 -t "$REGISTRY"/pagatu-gateway:latest --push .
	    SUCCESS_MSG="$SUCCESS_MSG gateway "
	    cd ..
    fi

    if [ "$var" = "mail" ] || [ "$var" = "all" ]
	  then
	    cd mail
	    mvn clean install -DskipTests
		  docker buildx build --platform linux/amd64,linux/arm64 -t "$REGISTRY"/pagatu-mail:latest --push .
	    SUCCESS_MSG="$SUCCESS_MSG mail "
	    cd ..
    fi
done

echo "$SUCCESS_MSG"
