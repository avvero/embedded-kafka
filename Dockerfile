FROM openjdk:17

COPY build/install/embedded-kafka-boot embedded-kafka-boot

RUN ls -al

EXPOSE 9093
EXPOSE 2181

ENTRYPOINT ["./embedded-kafka-boot/bin/embedded-kafka"]