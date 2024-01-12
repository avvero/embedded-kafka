####
# Build image
####
FROM openjdk:17 AS build
LABEL maintainer=avvero

RUN microdnf install findutils

COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

WORKDIR /app
COPY . .
RUN ./gradlew installBootDist --no-daemon

####
# Runtime image
####
FROM openjdk:17

COPY --from=build /app/build/install/embedded-kafka-boot embedded-kafka-boot

EXPOSE 8080 9093 2181

ENTRYPOINT ["./embedded-kafka-boot/bin/embedded-kafka"]