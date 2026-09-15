FROM eclipse-temurin:21-jre

WORKDIR /app

COPY encoding-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
