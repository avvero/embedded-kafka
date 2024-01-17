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
RUN ./gradlew emk-application:installBootDist --no-daemon

####
# Runtime image
####
FROM openjdk:17

COPY --from=build /app/emk-application/build/install/emk-application-boot emk-application-boot

EXPOSE 8080 9093 2181

ENTRYPOINT ["./emk-application-boot/bin/emk-application"]