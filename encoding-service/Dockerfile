FROM linuxserver/ffmpeg:latest AS ffmpeg-stage

FROM eclipse-temurin:21-jre

COPY --from=ffmpeg-stage /usr/local/bin/ffmpeg /usr/local/bin/ffmpeg

WORKDIR /app

COPY encoding-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
