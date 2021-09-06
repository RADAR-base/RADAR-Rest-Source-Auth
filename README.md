# RADAR-REST-Source-Auth

An application to get authorization from users to access their data through 3rd party APIs. Mainly supports OAuth2 Authorization code follow to request authorization and processes the returned authorization code to add new entries of users. It creates new users and adds properties compatible and required by the [RADAR REST and Fitbit connectors](https://github.com/RADAR-base/RADAR-REST-Connector).

## Features supported

1. It has one active entity where we store user properties.
2. Has liquibase support to enable seamless database schema migration.
3. Has a simple web-service with REST Endpoints to share configured source-type client details and authorized users.
4. Currently various source-types can be configured using the configuration file and these entries are stored in memory.

## APIs to be used by REST Source-Connectors

`RADAR REST Source-Connectors` can use the APIs as follows.

 1. To get all configured users for a particular source-type use `GET */users?source-type={source-type}` .
 2. To get details of a particular user use `GET */users/{id}`.
 3. To get the token details of a particular user use `GET */users/{id}/token`.
 4. To refresh the token of a particular user use `POST /users/{id}/token`.

## Installation

To install functional RADAR-base Rest-Sources Authorizer application with minimal dependencies from source, please use the `docker-compose.yml` under the root directory.

Copy the `docker/etc/rest-source-authorizer/authorizer.yml.template` into `docker/etc/rest-source-authorizer/authorizer.yml` and modify the `restSourceClients.FitBit.clientId` and `restSourceClients.FitBit.clientSecret` with your Fitbit client application credentials. Then start the docker-compose stack:

```bash
docker-compose up -d --build
```

You can find the Authorizer app running on `http://localhost:8080/rest-sources/authorizer/`. You can find the Management Portal app running on `http://localhost:8080/managementportal/`

## Authorization

All users registered with the application will be authorized against ManagementPortal for integrity and security.
Front-end application will perform additional validation based on regex to improve user experience.

### Registering OAuth Clients with ManagementPortal

To add new OAuth clients, you can add at runtime through the UI on Management Portal, or you can add them to the OAuth clients file referenced by the `MANAGEMENTPORTAL_OAUTH_CLIENTS_FILE` configuration option. For more info, see [officail docs](https://github.com/RADAR-base/ManagementPortal#oauth-clients)
The OAuth client for authorizer-app-backend should have the following properties.

```properties
client-id: radar_rest_sources_auth_backend
client-secret: Confidential
grant-type: client_credentials
resources: res_ManagementPortal
scope: PROJECT.READ,SUBJECT.READ
```

The OAuth client for authorizer-app should have the following properties.

```properties
client-id: radar_rest_sources_authorizer
client-secret: Empty
grant-type: authorization_code
resources: res_restAuthorizer
scope: SOURCETYPE.READ,PROJECT.READ,SUBJECT.READ,SUBJECT.UPDATE,SUBJECT.CREATE
callback-url: <advertised-url-of-rest-sources-authorizer-app>/login 
# the callback-url should be resolvable and match with the environment variable of radar-rest-sources-authorizer -> AUTH_CALLBACK_URL in the docker-compose.yml file. 
```

## Migrating from 1.\*.\* version to 2.\*

1. Move configurations from application.yml and environment variables to `authorizer.yml` following the description in `authorizer.yml.template`.
2. Move configurations from `rest_source_clients_configs.yml` to `restSourceClients` in corresponding YAML format in `authorizer.yml`.
