# Camigo（校速达）

[简体中文](README.md) | [English](README_EN.md)

Camigo 是校园智能快递与配送系统。本仓库采用 Monorepo 结构，同时管理 Spring Boot 后端和 Vue 3 前端。

## 项目结构

```text
camigo/
├── camigo-server/    # Spring Boot 3 + MyBatis-Plus 后端
├── camigo-web/       # Vue 3 + Vite + TypeScript 前端
├── .gitignore
├── README.md
└── README_EN.md
```

前后端仍是两个独立应用，分别安装依赖、构建和启动。

## 环境要求

- Java 17
- Maven 3.9+
- Node.js 20+
- MySQL 8.0
- Redis 或兼容 Redis 协议的 Memurai

## 启动后端

先在本机配置 `MYSQL_USERNAME`、`MYSQL_PASSWORD` 和不少于 32 字节的 `JWT_SECRET`，不要把真实值提交到 Git。

```shell
cd camigo-server
mvn test
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`。

## 启动前端

```shell
cd camigo-web
npm install
npm run dev
```

前端默认地址：`http://127.0.0.1:5173`，默认调用 `http://localhost:8080/api`。

## 构建检查

```shell
cd camigo-server
mvn test

cd ../camigo-web
npm run build
```

各应用的详细设计与使用说明分别位于对应子目录的 `README.md` 和 `docs/`。
