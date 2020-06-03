# Build stage
FROM openjdk:8-jdk-alpine as builder

WORKDIR /app
COPY gradlew /app/
COPY gradle/wrapper gradle/wrapper
COPY gradle gradle
COPY build.gradle settings.gradle /app/
RUN ./gradlew --version downloadDependencies

COPY src src
RUN ./gradlew assemble
# Run stage
FROM openjdk:8-jre-alpine

# Add the war and changelogs files from build stage
COPY --from=builder app/build/libs/*.jar /app.jar

EXPOSE 8080
CMD echo "The application will start in ${APP_SLEEP}s..." && \
    sleep ${APP_SLEEP} && \
    java -Djava.security.egd=file:/dev/./urandom -jar /app.jar
