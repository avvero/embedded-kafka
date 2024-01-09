####
# Build image
####
FROM openjdk:17 AS build
LABEL maintainer=avvero

RUN microdnf install findutils

WORKDIR /app
COPY . .
RUN ./gradlew installBootDist --no-daemon

####
# Runtime image
####
FROM openjdk:17

COPY --from=build /app/build/install/embedded-kafka-boot embedded-kafka-boot

RUN ls -al

EXPOSE 8080
EXPOSE 9093
EXPOSE 2181

ENTRYPOINT ["./embedded-kafka-boot/bin/embedded-kafka"]