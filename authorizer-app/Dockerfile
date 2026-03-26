FROM --platform=$BUILDPLATFORM node:18-alpine as builder

RUN mkdir /app
WORKDIR /app

COPY authorizer-app/package.json authorizer-app/yarn.lock /app/
RUN yarn install

COPY authorizer-app/ /app/

RUN yarn build

FROM nginxinc/nginx-unprivileged:1.27-alpine3.20-perl

ENV BASE_HREF="/rest-sources/authorizer/" \
    BACKEND_BASE_URL="http://localhost/rest-sources/backend" \
    AUTH_GRANT_TYPE="authorization_code" \
    AUTH_CLIENT_ID="radar_rest_sources_authorizer" \
    AUTH_CLIENT_SECRET="" \
    AUTH_CALLBACK_URL="http://localhost/rest-sources/authorizer/login" \
    AUTH_URI="http://localhost/managementportal/oauth" \
    AUTH_PATH="/authorize" \
    RADAR_BASE_URL="http://localhost"

# add init script
COPY authorizer-app/docker/optimization.conf /etc/nginx/conf.d/
COPY --chown=101 authorizer-app/docker/default.conf /etc/nginx/conf.d/
COPY authorizer-app/docker/30-env-subst.sh /docker-entrypoint.d/

WORKDIR /usr/share/nginx/html

COPY --from=builder /app/dist/ .
COPY --from=builder --chown=101 /app/dist/main.* /app/dist/index.html* ./
