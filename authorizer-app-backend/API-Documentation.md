#API Documentation

## API documentation

1. Request for authorized projects

```bash
GET /projects
```
Response format
```json
{
  "projects": [
    {
      "id": "test",
      "location": "test",
      "description": "test"
    }
  ]
}
```
2. Request for project details by id
```bash
GET /projects/test
```
Response format
```json
{
  "id": "test",
  "location": "test",
  "description": "test"
}
```
3. Requests for subjects/participants of a project
```bash
GET /projects/test/users
```
Response format
```json
{
  "users": [
    {
      "id": "628277bb-239e-4137-9d15-9d8f6bb05618",
      "projectId": "test",
      "status": "ACTIVATED"
    }
  ]
}
```
