#!/bin/bash

set -e

cd "$(dirname "$0")"
cd ..

cd auth
mvn clean install -DskipTests
cd ..

cd coffee
mvn clean install -DskipTests
cd ..

cd eureka-server
mvn clean install -DskipTests
cd ..

cd gateway-service
mvn clean install -DskipTests
cd ..

cd mail
mvn clean install -DskipTests
cd ..
