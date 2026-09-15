FROM eclipse-temurin:21-jre

WORKDIR /app

COPY streaming-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
