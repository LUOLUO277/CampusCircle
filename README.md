# Campus Circle 2.0

This repository contains the full local-development workspace for the project:

- `backend/`: Spring Boot backend
- `frontend/`: uni-app frontend

This README is written for teammates who need to clone the repo, configure a local database, and start developing quickly on their own machines.

## 1. Recommended setup

Recommended for first-time setup:

1. Run the backend with the default local profile.
2. Use the built-in local H2 database first.
3. Start the frontend after the backend is up.

This path is the lowest-friction option because it does not require a shared server or a shared remote database.

## 2. Prerequisites

Backend:

- JDK 21
- Maven 3.9+
- Redis on `localhost:6379` is recommended

Frontend:

- HBuilderX

Optional for backend MySQL mode:

- MySQL 8+

## 3. Backend local database options

The backend supports two local development modes.

### Option A: Default local mode with H2

This is the default mode now.

Key config files:

- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-local.properties`

Important settings:

- Default active profile: `local`
- H2 database file: `backend/data/campus-circle.mv.db`
- Upload directory: `backend/uploads`
- Log file: `backend/logs/campus-backend.log`
- H2 console: `http://localhost:8080/h2-console`

Start command:

```powershell
cd backend
mvn spring-boot:run
```

What happens on first startup:

- JPA auto-creates tables
- Default categories are initialized
- Default admin user is initialized

Default initialized account:

- Username: `admin`
- Password: `admin123`

### Option B: Local MySQL mode

Key config file:

- `backend/src/main/resources/application-local-mysql.properties`

Recommended steps:

1. Make sure local MySQL is installed and running.
2. Create a local database named `campus_circle`, or let Spring create it automatically.
3. Start the backend with the `local-mysql` profile.

Example:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="local-mysql"
$env:LOCAL_DB_URL="jdbc:mysql://localhost:3306/campus_circle?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&createDatabaseIfNotExist=true"
$env:LOCAL_DB_USERNAME="root"
$env:LOCAL_DB_PASSWORD="your_password"
mvn spring-boot:run
```

Related helper script:

- `backend/scripts/init-local-mysql.sql`

Notes:

- `spring.jpa.hibernate.ddl-auto=update` is enabled in local MySQL mode.
- Each teammate can use a separate local database account and local data set.

## 4. Redis notes

Redis is used for online status and token blacklist behavior.

Configs:

- H2 local mode: `backend/src/main/resources/application-local.properties`
- MySQL local mode: `backend/src/main/resources/application-local-mysql.properties`

Default:

- Host: `localhost`
- Port: `6379`

If Redis is not available, the backend has already been adjusted so core development is still possible. Some online-status and blacklist behavior will degrade gracefully.

## 5. Backend startup checklist

After starting the backend, verify:

1. Open `http://localhost:8080/swagger-ui.html`
2. Open `http://localhost:8080/actuator/health`
3. If using H2, open `http://localhost:8080/h2-console`

## 6. Frontend startup

Open `frontend/` in HBuilderX and run it there.

The frontend now defaults to calling:

```text
http://localhost:8080/api
```

If you are debugging on the same computer, no extra change is needed.

## 7. Frontend real-device debugging

If you run the frontend on a phone, `localhost` will point to the phone itself, not your computer.

Set the backend origin to your computer LAN IP:

```js
import { setDevBackendOrigin } from '@/utils/api'
setDevBackendOrigin('http://192.168.1.23:8080')
```

Reset to default:

```js
import { clearDevBackendOrigin } from '@/utils/api'
clearDevBackendOrigin()
```

## 8. Where unified addresses are defined now

### Unified API base address

Primary definition:

- `frontend/utils/api.js`
  - `DEFAULT_BACKEND_ORIGIN`
  - `getApiBaseUrl()`

Runtime request usage:

- `frontend/utils/request.js`
- `frontend/api/user.js`

### Unified absolute image address

Primary helper:

- `frontend/utils/api.js`
  - `toAbsoluteUrl()`

Current usage:

- `frontend/components/PostCard.vue`
- `frontend/pages/post/detail.vue`
- `frontend/pages/errand/detail.vue`

### Unified upload address

Current definitions:

- `frontend/utils/request.js`
  - `upload()`
- `frontend/api/common.js`
  - `/common/upload`
  - `/common/upload/batch`

Backend upload endpoints:

- `backend/src/main/java/com/campus/campus_backend/controller/CommonController.java`

## 9. Suggested local collaboration workflow

Because there is no shared database and no shared test data, use this workflow:

1. Keep API contracts stable first.
2. Add or update example request and response payloads in PR descriptions.
3. Avoid relying on local database IDs being identical across machines.
4. If backend changes an API field, update the frontend in the same branch.
5. Seed minimal local data through startup initialization or explicit scripts.

More detailed collaboration guidance is in:

- `DEVELOPMENT_GUIDE.md`

## 10. Repository structure

```text
campus_circle2.0/
  backend/
  frontend/
  README.md
  DEVELOPMENT_GUIDE.md
  ADDRESS_REFERENCE.md
```
