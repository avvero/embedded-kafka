####
# Build image
####
FROM openjdk:17 AS build
LABEL maintainer=avvero

WORKDIR /app
COPY . .
RUN ./gradlew installBootDist

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