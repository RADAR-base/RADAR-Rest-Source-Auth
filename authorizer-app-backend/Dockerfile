# Build stage
FROM gradle:6.6.1-jdk11 as builder


RUN mkdir /code
WORKDIR /code

COPY ./build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY authorizer-app-backend/build.gradle.kts  /code/authorizer-app-backend/
RUN gradle :authorizer-app-backend:downloadDependencies

COPY authorizer-app-backend/src /code/authorizer-app-backend/src

RUN gradle -Dkotlin.compiler.execution.strategy="in-process" -Dorg.gradle.parallel=false -Pkotlin.incremental=false :authorizer-app-backend:distTar \
    && cd authorizer-app-backend/build/distributions \
    && tar xf *.tar \
    && rm *.tar authorizer-app-backend-*/lib/authorizer-app-backend-*.jar

FROM openjdk:11-jre-slim

MAINTAINER @nivemaham @blootsvoets

LABEL description="RADAR-base rest sources authorizer backend application"

COPY --from=builder /code/authorizer-app-backend/build/distributions/authorizer-app-backend-*/bin/* /usr/bin/
COPY --from=builder /code/authorizer-app-backend/build/distributions/authorizer-app-backend-*/lib/* /usr/lib/
COPY --from=builder /code/authorizer-app-backend/build/libs/authorizer-app-backend-*.jar /usr/lib/

EXPOSE 8085

CMD ["authorizer-app-backend"]

