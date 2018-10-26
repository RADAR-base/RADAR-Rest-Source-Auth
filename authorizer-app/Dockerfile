FROM node:8.12.0-alpine as builder

WORKDIR /app

COPY package.json /app/
RUN npm install

COPY ./ /app/

RUN $(npm bin)/ng build --prod


FROM nginx:1.15.5-alpine

# add init script
COPY ./docker/nginx.conf /etc/nginx/nginx.conf

WORKDIR /usr/share/nginx/html

COPY --from=builder /app/dist/authorizer-app/ .

# expose internal port:80 and run init.sh
EXPOSE 80
