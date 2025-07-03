# Dockerfile
FROM eclipse-temurin:21-jdk

# JAR 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
