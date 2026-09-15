FROM jrottenberg/ffmpeg:7.1-ubuntu2404 AS ffmpeg-stage

FROM eclipse-temurin:21-jre

COPY --from=ffmpeg-stage /usr/local/bin/ffmpeg /usr/local/bin/ffmpeg
COPY --from=ffmpeg-stage /usr/local/lib /usr/local/lib

RUN ldconfig

WORKDIR /app

COPY encoding-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
