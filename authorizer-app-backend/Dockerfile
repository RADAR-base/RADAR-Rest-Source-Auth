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

FROM --platform=$BUILDPLATFORM gradle:8.13-jdk17 as builder

RUN mkdir /code
WORKDIR /code

ENV GRADLE_USER_HOME=/code/.gradlecache \
    GRADLE_OPTS="-Djdk.lang.Process.launchMechanism=vfork -Dorg.gradle.vfs.watch=false"

COPY ./buildSrc /code/buildSrc
COPY ./build.gradle.kts ./settings.gradle.kts ./gradle.properties /code/
COPY ./buildSrc /code/buildSrc
COPY authorizer-app-backend/build.gradle.kts  /code/authorizer-app-backend/
RUN gradle downloadDependencies copyDependencies startScripts

COPY authorizer-app-backend/src /code/authorizer-app-backend/src

RUN gradle jar

FROM eclipse-temurin:17-jre

LABEL description="RADAR-base rest sources authorizer backend application"

# Override JAVA_OPTS to set heap parameters, for example
ENV JAVA_OPTS="" \
    AUTHORIZER_APP_BACKEND_OPTS="" \
    # Globally disables liquibase analytics.
    # (see: https://docs.liquibase.com/pro/user-guide/what-data-does-liquibase-collect-and-how-is-it-used)
    LIQUIBASE_ANALYTICS_ENABLED=false

COPY --from=builder /code/authorizer-app-backend/build/scripts/* /usr/bin/
COPY --from=builder /code/authorizer-app-backend/build/third-party/* /usr/lib/
COPY --from=builder /code/authorizer-app-backend/build/libs/authorizer-app-backend-*.jar /usr/lib/

USER 101

EXPOSE 8085

CMD ["authorizer-app-backend"]
