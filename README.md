# RADAR-Device-Auth

An application to get authorization from device users to access their data through 3rd party APIs. Mainly supports OAuth2 Authorization code follow to request authorization and processes the returned authorization code to add new entries of users. It creates new users and adds properties compatible and required by the RADAR-REST-CONNECTOR/RADAR-FITBIT-REST-CONNECTOR.

## Features supported
1. It has one active entity where we store user properties.
2. Has liquibase support to enable seamless database schema migration.
3. Has a simple web-service with REST Endpoints to share configured device-type details and authorized users.
4. Currently various device-types can be configured using a YAML file and these entries are stored in memory.

## APIs to be used by Device-Connectors
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