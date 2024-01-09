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

# Native build command
native-build:
	./gradlew buildNative

# Docker build command for standard Dockerfile
docker-build:
	docker build -t $(DOCKER_REPO)/$(IMAGE_NAME):latest -f Dockerfile .
	docker build -t $(DOCKER_REPO)/$(IMAGE_NAME):$(VERSION) -f Dockerfile .

# Docker build command for native Dockerfile
docker-build-native:
	docker build -t $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest -f Dockerfile.native .
	docker build -t $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):$(VERSION) -f Dockerfile.native .

# Docker push command for standard image
docker-push:
	docker push $(DOCKER_REPO)/$(IMAGE_NAME):latest
	docker push $(DOCKER_REPO)/$(IMAGE_NAME):$(VERSION)

# Docker push command for native image
docker-push-native:
	docker push $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):latest
	docker push $(DOCKER_REPO)/$(NATIVE_IMAGE_NAME):$(VERSION)

.PHONY: run test native-build docker-build docker-build-native docker-push docker-push-native