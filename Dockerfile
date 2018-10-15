# Run stage
FROM openjdk:8-jre-alpine

# Add the war and changelogs files from build stage
COPY ./build/libs/*.jar /app.jar
COPY ./src/main/docker/ /app-includes

EXPOSE 8080
CMD echo "The application will start in ${APP_SLEEP}s..." && \
    sleep ${APP_SLEEP} && \
    java -Djava.security.egd=file:/dev/./urandom -jar /app.jar
