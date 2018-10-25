# RADAR-Device-Auth

This repository supports device authorization and maintains user properties related to authorized devices.
It includes a webservice which provides `REST endpoints` to enable authorization process and device-user management.

Device `Source-Connectors` can use the APIs as follows.
 1. To get all configured users for a particular device-type use `GET */users/{device-type}` .
 2. To get details of a particular user use `GET */users/{id}`.
 3. To get the token details of a particular user use `GET */users/{id}/token`.
 4. To refresh the token of a particular user use `POST /users/{id}/token`.

## Usage
To run this application from source:

```$cmd
./gradlew build assemble
java -jar radar-device-authorizer*.jar
```

To build and run this application from Docker:
```$cmd

docker build -t radarbase/radar-device-auth-backend:latest .

docker run -p 8080:8080 radarbase/radar-device-auth-backend:latest
```