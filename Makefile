# Makefile

# Variables for Docker image names
IMAGE_NAME := emk
NATIVE_IMAGE_NAME := emk-native
DOCKER_REPO := avvero
VERSION := $(shell grep '^version=' gradle.properties | cut -d '=' -f2)

test:
	./gradlew test

emk-run:
	./gradlew emk-application:bootRun

emk-testcontainers-deploy:
	mvn -f emk-testcontainers/pom.xml deploy

emk-run-with-agent:
	./gradlew emk-application:installBootDist
	java -agentlib:native-image-agent=config-output-dir=emk-application/src/main/resources/META-INF/native-image -jar emk-application/build/libs/emk-application-${VERSION}.jar

# Native build command
emk-native-build:
	./gradlew emk-application:nativeCompile

# Docker build command for standard Dockerfile
emk-docker-build:
	docker build -t $(DOCKER_REPO)/$(IMAGE_NAME):latest -f Dockerfile .
	docker tag $(DOCKER_REPO)/$(IMAGE_NAME):latest $(DOCKER_REPO)/$(IMAGE_NAME):$(VERSION)

# Docker push command for standard image
emk-docker-push:
	docker push $(DOCKER_REPO)/$(IMAGE_NAME):latest
	docker push $(DOCKER_REPO)/$(IMAGE_NAME):$(VERSION)

# Docker build command for native Dockerfile
emk-docker-build-native:
	docker build -t $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest -f Dockerfile.native .
	docker tag $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):$(VERSION)

# Docker push command for native image
emk-docker-push-native:
	docker push $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest
	docker push $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):$(VERSION)

.PHONY: test emk-run emk-run-with-agent emk-native-build emk-docker-build emk-docker-build-native emk-docker-push emk-docker-push-native emk-testcontainers-publish