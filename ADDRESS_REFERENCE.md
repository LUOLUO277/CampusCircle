# Address Reference

This file records where the unified local-development addresses are defined.

## 1. Unified API base address

Definition:

- `frontend/utils/api.js`
  - `DEFAULT_BACKEND_ORIGIN = 'http://localhost:8080'`
  - `getApiBaseUrl()`

Request entry:

- `frontend/utils/request.js`
  - `getBaseUrl()`

Frontend API modules already using it:

- `frontend/api/user.js`
- `frontend/api/common.js`

## 2. Unified absolute image address

Definition:

- `frontend/utils/api.js`
  - `toAbsoluteUrl()`

Current consumers:

- `frontend/components/PostCard.vue`
- `frontend/pages/post/detail.vue`
- `frontend/pages/errand/detail.vue`

## 3. Unified upload address

Frontend definitions:

- `frontend/utils/request.js`
  - `upload()`
- `frontend/api/common.js`
  - `uploadImage()`
  - `uploadBatchImages()`

Backend endpoints:

- `backend/src/main/java/com/campus/campus_backend/controller/CommonController.java`
  - `POST /api/common/upload`
  - `POST /api/common/upload/batch`

## 4. Backend local service address

Definition:

- `backend/src/main/resources/application.properties`
  - `server.port=8080`
- `backend/src/main/resources/application-local.properties`
  - `server.url=http://localhost:8080`
- `backend/src/main/resources/application-local-mysql.properties`
  - `server.url=http://localhost:8080`

## 5. Real-device override

If mobile debugging is needed:

- Use `frontend/utils/api.js`
  - `setDevBackendOrigin('http://<your-lan-ip>:8080')`
  - `clearDevBackendOrigin()`
