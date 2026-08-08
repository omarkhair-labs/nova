# Nova backend

The Nova API is a Django REST Framework service backed by PostgreSQL.

## Start locally

From the repository root:

```powershell
docker compose up --build
```

The API will be available at `http://localhost:8000`.

Health check:

```text
GET /api/v1/health/
```

Authentication endpoints:

```text
POST /api/v1/auth/register/
POST /api/v1/auth/login/
POST /api/v1/auth/refresh/
GET  /api/v1/me/
PATCH /api/v1/me/
```

## Run tests

With the database container running:

```powershell
docker compose run --rm api python manage.py test
```

## Notes

- Email is the login identifier.
- Usernames are unique and normalized to lowercase.
- Passwords use Django's password hashing and validators.
- Access tokens expire after 15 minutes.
- Refresh tokens expire after 30 days.
- The current credentials in `docker-compose.yml` are development-only defaults.
