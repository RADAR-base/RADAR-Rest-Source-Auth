FROM --platform=$BUILDPLATFORM node:16-alpine as builder

ARG BASE_HREF="BASE_HREF"

RUN mkdir /app
WORKDIR /app

COPY package.json yarn.lock /app/
RUN yarn install

COPY ./ /app/

RUN yarn build --base-href=${BASE_HREF}

FROM nginx:1.20-alpine

# add init script
COPY ./docker/nginx.conf /etc/nginx/nginx.conf

WORKDIR /usr/share/nginx/html

COPY --from=builder /app/dist/authorizer-app/ .

COPY ./docker/entrypoint.sh /entrypoint.sh
# expose internal port:80 and run init.sh
EXPOSE 80

ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
