FROM --platform=$BUILDPLATFORM node:18-alpine as builder

RUN mkdir /app
WORKDIR /app

COPY package.json yarn.lock /app/
RUN yarn install

COPY ./ /app/

RUN yarn build

FROM nginxinc/nginx-unprivileged:1.24-alpine

ENV BASE_HREF="/rest-sources/authorizer/" \
    BACKEND_BASE_URL="http://localhost/rest-sources/backend" \
    AUTH_GRANT_TYPE="authorization_code" \
    AUTH_CLIENT_ID="radar_rest_sources_authorizer" \
    AUTH_CLIENT_SECRET="" \
    AUTH_CALLBACK_URL="http://localhost/rest-sources/authorizer/login" \
    AUTH_URI="http://localhost/managementportal/oauth" \
    RADAR_BASE_URL="http://localhost"

# add init script
COPY docker/optimization.conf /etc/nginx/conf.d/
COPY --chown=101 docker/default.conf /etc/nginx/conf.d/
COPY docker/30-env-subst.sh /docker-entrypoint.d/

WORKDIR /usr/share/nginx/html

COPY --from=builder /app/dist/ .
COPY --from=builder --chown=101 /app/dist/main.* /app/dist/index.html* ./
