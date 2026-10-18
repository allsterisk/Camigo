# Camigo

[简体中文](README.md) | [English](README_EN.md)

Camigo is a smart campus parcel management and delivery system. This monorepo contains both the Spring Boot backend and the Vue 3 frontend.

## Project Structure

```text
camigo/
├── camigo-server/    # Spring Boot 3 + MyBatis-Plus backend
├── camigo-web/       # Vue 3 + Vite + TypeScript frontend
├── .gitignore
├── README.md
└── README_EN.md
```

The backend and frontend remain independent applications and should be installed, built, and started separately.

## Requirements

- Java 17
- Maven 3.9+
- Node.js 20+
- MySQL 8.0
- Redis or Memurai with Redis protocol compatibility

## Start the Backend

Configure `MYSQL_USERNAME`, `MYSQL_PASSWORD`, and a `JWT_SECRET` of at least 32 bytes in your local environment. Never commit real credentials to Git.

```shell
cd camigo-server
mvn test
mvn spring-boot:run
```

The backend is available at `http://localhost:8080` by default.

## Start the Frontend

```shell
cd camigo-web
npm install
npm run dev
```

The frontend is available at `http://127.0.0.1:5173` by default and calls `http://localhost:8080/api`.

## Build Verification

```shell
cd camigo-server
mvn test

cd ../camigo-web
npm run build
```

More detailed design and usage documentation is available in each application's `README.md` and the backend `docs/` directory.
