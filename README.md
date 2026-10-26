# AuthService

Spring Boot microservice that manages access to Userservice.
Educational task by Innowise.

## Stack

Java 21, Spring Boot 4.1.1, PostgreSQL, Liquibase, Docker.

## Entities

- **Credentials**: id, userId, username, password, role
- **Role**: User, Admin
- 
## Run with Docker

```bash
git clone <repo-url>
cd <repo-folder>
docker compose up --build
```

The stack starts PostgreSQL and the service (profile `docker`).

- Service: http://localhost:8082
- PostgreSQL: `jdbc:postgresql://localhost:5556/authdb`
  (user `postgres`, password `demo`)


```bash
./gradlew bootRun
```

## Configuration

Profiles: `local` (default) and `docker`.
