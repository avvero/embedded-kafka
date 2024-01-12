# Makefile

# Variables for Docker image names
IMAGE_NAME := emk
NATIVE_IMAGE_NAME := emk-native
DOCKER_REPO := avvero
VERSION := $(shell grep '^version=' gradle.properties | cut -d '=' -f2)

# Gradle service run command
run:
	./gradlew bootRun

test:
	./gradlew test

run-with-agent:
	./gradlew installBootDist
	java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar build/libs/embedded-kafka-${VERSION}.jar --app.kafka.startup-mode=at-once

# Native build command
native-build:
	./gradlew nativeCompile

# Docker build command for standard Dockerfile
docker-build:
	docker build -t $(DOCKER_REPO)/$(IMAGE_NAME):latest -f Dockerfile .
	docker tag $(DOCKER_REPO)/$(IMAGE_NAME):latest $(DOCKER_REPO)/$(IMAGE_NAME):$(VERSION)

# Docker push command for standard image
docker-push:
	docker push $(DOCKER_REPO)/$(IMAGE_NAME):latest
	docker push $(DOCKER_REPO)/$(IMAGE_NAME):$(VERSION)

# Docker build command for native Dockerfile
docker-build-native:
	docker build --platform=linux/arm64 -t $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest -f Dockerfile.native .
	docker tag $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):$(VERSION)

# Docker push command for native image
docker-push-native:
	docker push $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest
	docker push $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):$(VERSION)

.PHONY: run test native-build docker-build docker-build-native docker-push docker-push-native