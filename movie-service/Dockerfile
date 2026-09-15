FROM eclipse-temurin:21-jre

WORKDIR /app

COPY movie-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
