Updated authorization flow:

- [x] created API
- [ ] frontend flow

The API can be used as follows:

- press create user
- select source type, project, user ID, start date, end date
- request `POST <backend url>/users` with contents
    ```ts
    interface RestSourceUserRequest {
        projectId: String
        userId: String
        sourceId: String
        startDate: Instant
        endDate?: Instant
        sourceType: String
    }
    ```
  which will have response (for user 1)
    ```
    HTTP 201 Created
    Location: <backend url>/users/1
    ```
    ```ts
    interface RestSourceUserResponse {
        id: String
        projectId: String
        userId: String
        sourceId: String
        startDate: Instant
        endDate?: Instant
        sourceType: String
        createdAt: Instant
        humanReadableUserId: String
        externalId: String
        serviceUserId?: String
        isAuthorized: Boolean
        timesReset: Int
        version: String
    }
    ```

- After the user has been created, an account registration request can be created at `POST <backend url>/registrations` with contents

     ```ts
     interface RegistrationCreateRequest {
         userId: String  // matches RestSourceUserResponse.id
         persistent?: Boolean  // set to true to get a long-living token
     }
     ```

  with response

     ```ts
     interface RegistrationResponse {
         token: String
         secret?: String  // only defined if the registration is persistent
         userId: String
         authEndpointUrl?: String  // only defined if the registration is not persistent
         expiresAt: Instant
         persistent: Boolean
     }
     ```

- The token and secret can be used in a frontend URL to pass to the backend at a later time, or the `authEndpointUrl` can used immediately.

    - If the user created a persistent token, the `authEndpointUrl` can be retrieved with a request to `POST <backend url>/registrations/<token>` with contents

        ```ts
        interface RegistrationRequest {
            secret: String
        }
        ```

      and it will return the authorization URL to be used:

        ```ts
        interface RegistrationResponse {
            token: String
            userId: String
            authEndpointUrl: String
            expiresAt: Instant
            persistent: Boolean
        }
        ```

- When the user is authorized, they will be redirected to `<frontend url>/users:new` with a number of query parameters, including the `state` parameter. To finalize the authentication procedure, call `POST <backend url>/registrations/<state>/authorize` with contents

    ```ts
    interface AuthorizeRequest {
        code?: String
        oauth_token?: String
        oauth_verifier?: String
        oauth_token_secret?: String
    }
    ```

  which on success will respond with the location of `<backend url>/source-clients/<sourceType>/authorization/<service user ID>` and with as contents `RegistrationResponse` without the secret and endpoint URL.

    - If the response indicated that the token was persistent, the user that can simply be thanked for entering their user details.
    - Otherwise, the user can be redirected to the list of users.
    - In both cases, the registration is now removed.
