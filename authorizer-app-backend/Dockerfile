# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM gradle:7.3-jdk17 as builder

RUN mkdir /code
WORKDIR /code
ENV GRADLE_USER_HOME=/code/.gradlecache
ENV GRADLE_OPTS=-Djdk.lang.Process.launchMechanism=vfork

COPY ./build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY authorizer-app-backend/build.gradle.kts  /code/authorizer-app-backend/
RUN gradle :authorizer-app-backend:downloadDependencies --no-watch-fs

COPY authorizer-app-backend/src /code/authorizer-app-backend/src

RUN gradle --no-watch-fs :authorizer-app-backend:distTar \
    && cd authorizer-app-backend/build/distributions \
    && tar xf *.tar \
    && rm *.tar authorizer-app-backend-*/lib/authorizer-app-backend-*.jar

FROM azul/zulu-openjdk-alpine:17.0.2-jre-headless

LABEL description="RADAR-base rest sources authorizer backend application"

# Override JAVA_OPTS to set heap parameters, for example
ENV JAVA_OPTS="" \
    AUTHORIZER_APP_BACKEND_OPTS=""

RUN apk add --no-cache curl

COPY --from=builder /code/authorizer-app-backend/build/distributions/authorizer-app-backend-*/bin/* /usr/bin/
COPY --from=builder /code/authorizer-app-backend/build/distributions/authorizer-app-backend-*/lib/* /usr/lib/
COPY --from=builder /code/authorizer-app-backend/build/libs/authorizer-app-backend-*.jar /usr/lib/

USER 101

EXPOSE 8085

CMD ["authorizer-app-backend"]

