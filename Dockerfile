FROM openjdk:17

COPY build/install/embedded-kafka-boot embedded-kafka-boot

RUN ls -al

EXPOSE 55900

ENTRYPOINT ["./embedded-kafka-boot/bin/embedded-kafka"]