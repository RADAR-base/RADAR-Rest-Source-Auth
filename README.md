# RADAR-REST-Source-Auth

An application to get authorization from users to access their data through 3rd party APIs. Mainly supports OAuth2 Authorization code follow to request authorization and processes the returned authorization code to add new entries of users. It creates new users and adds properties compatible and required by the RADAR-REST-CONNECTOR/RADAR-FITBIT-REST-CONNECTOR.

## Features supported
1. It has one active entity where we store user properties.
2. Has liquibase support to enable seamless database schema migration.
3. Has a simple web-service with REST Endpoints to share configured source-type client details and authorized users.
4. Currently various source-types can be configured using a YAML file and these entries are stored in memory.

## APIs to be used by REST Source-Connectors
`RADAR REST Source-Connectors` can use the APIs as follows.
 1. To get all configured users for a particular source-type use `GET */users/{source-type}` .
 2. To get details of a particular user use `GET */users/{id}`.
 3. To get the token details of a particular user use `GET */users/{id}/token`.
 4. To refresh the token of a particular user use `POST /users/{id}/token`.

## Usage
To run this application from source:

```$cmd
./gradlew build assemble
java -jar radar-rest-sources-authorizer*.jar
```
## Installation
To install functional RADAR-base Rest-Sources Authorizer application with minimal dependencies from source, please use the `docker-compose.yml` under the root directory
1. Copy the `docker/etc/rest-sources-authorizer/rest_source_clients_configs.yml.template` into `docker/etc/rest-sources-authorizer/rest_source_clients_configs.yml` and modify the `client_id` and `client_secret` with your Fitbit client application credentials.
```bash
docker-compose up -d --build
```

## Validation

There is validation available for the properties of the subject entered by the user. These are currenlty validated using the details from the Management portal. You can configure this according to your requirements as follows -

### If don't need validation
Add the `REST_SOURCE_AUTHORIZER_VALIDATOR` env var to your docker-compose service to disable validation-
```yaml
  radar-rest-sources-backend:
    image: radarbase/radar-rest-source-auth-backend:1.2.1
...
    environment:
...
      - REST_SOURCE_AUTHORIZER_VALIDATOR=""
    volumes:
      - ./etc/rest-source-authorizer/:/app-includes/
...

```
**Note: This will only disable backend validation. The frontend validation(based on Regex) will still exist.**

### Enable validation using Management Portal

#### First Create a new oAuth client in Management Portal
To add new OAuth clients, you can add at runtime through the UI on Management Portal, or you can add them to the OAuth clients file referenced by the MANAGEMENTPORTAL_OAUTH_CLIENTS_FILE configuration option. For more info, see [officail docs](https://github.com/RADAR-base/ManagementPortal#oauth-clients)
The OAuth client should have the following properties-

1. scope - `PROJECT.READ, SUBJECT.READ`
2. grant_type - `client_credentials`

#### Then add the following to your rest authoriser service
Add the following env vars to your docker-compose service-
```yaml
  radar-rest-sources-backend:
    image: radarbase/radar-rest-source-auth-backend:1.2.1
...
    environment:
...
      - REST_SOURCE_AUTHORIZER_VALIDATOR=managementportal
      - REST_SOURCE_AUTHORIZER_MANAGEMENT_PORTAL_BASE_URL=http://managementportal-app:8080/managementportal/
      - REST_SOURCE_AUTHORIZER_MANAGEMENT_PORTAL_OAUTH_CLIENT_ID=radar_rest_sources_auth
      - REST_SOURCE_AUTHORIZER_MANAGEMENT_PORTAL_OAUTH_CLIENT_SECRET=secret
    volumes:
      - ./etc/rest-source-authorizer/:/app-includes/
...
```

**Note**: Make sure to configure the client id and client secret as created in the Management portal
