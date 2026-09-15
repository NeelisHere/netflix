FROM eclipse-temurin:21-jre

WORKDIR /app

COPY video-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
