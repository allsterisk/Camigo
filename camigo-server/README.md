
# Camigo Server

Camigo（校速达）后端，当前已完成 Phase 4 基础框架及第九阶段业务模块。

## 技术基线

- Java 17
- Spring Boot 3.5.16
- Maven
- MyBatis-Plus 3.5.17
- MySQL 8.0
- Redis
- SpringDoc OpenAPI

## 依赖说明

| 依赖 | 作用 |
|---|---|
| `spring-boot-starter-web` | REST API、JSON 序列化和嵌入式 Tomcat |
| `spring-boot-starter-validation` | DTO 与请求参数校验 |
| `mybatis-plus-spring-boot3-starter` | Mapper、基础 CRUD 和 MyBatis-Plus 自动配置 |
| `mybatis-plus-jsqlparser` | 分页插件需要的 SQL 解析能力 |
| `mysql-connector-j` | MySQL JDBC 驱动 |
| `spring-boot-starter-data-redis` | Redis 连接与 `RedisTemplate` |
| `springdoc-openapi-starter-webmvc-ui` | OpenAPI 3 与 Swagger UI |
| `jjwt-*` | JWT 创建、解析、验签与 JSON 支持 |
| `spring-security-crypto` | BCrypt 密码摘要 |
| `lombok` | 减少简单模型中的样板代码 |
| `spring-boot-starter-test` | JUnit 5、Mockito、AssertJ 和 Spring Test |

## 必需环境变量

```text
MYSQL_USERNAME=数据库用户名
MYSQL_PASSWORD=数据库密码
JWT_SECRET=至少32字节的随机密钥
```

可选变量包括 `MYSQL_URL`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE`、`SERVER_PORT` 和 `JWT_EXPIRATION_MINUTES`。

不要把真实密码或 JWT 密钥写入 `application.yml`、Git 或日志。

## 开发命令

```shell
mvn clean test
mvn spring-boot:run
```

应用启动后访问：

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## Vue3 前端联调

前端项目位于同一仓库的 `../camigo-web`，默认从 `http://localhost:8080/api` 调用本服务。后端允许 `http://localhost:5173` 和 `http://127.0.0.1:5173` 的 Vite 开发请求，并支持 `Authorization` Header。

```shell
cd ../camigo-web
npm install
npm run dev
```

## 已实现接口

| 模块 | 方法与路径 | 功能 |
|---|---|---|
| 用户 | `POST /api/users/register` | 注册并绑定 USER 角色 |
| 用户 | `POST /api/users/login` | 登录并返回 JWT |
| 用户 | `GET /api/users/info` | 当前用户资料 |
| 用户 | `PUT /api/users/{id}` | 本人或管理员修改用户资料 |
| 快递 | `POST /api/parcels` | 管理员录入快递 |
| 快递 | `GET /api/parcels` | 管理员分页查询快递 |
| 快递 | `GET /api/parcels/my` | 查询当前用户快递 |
| 快递 | `GET /api/parcels/{id}` | 按对象权限查询快递详情 |
| 快递 | `PUT /api/parcels/{id}/status` | 更新状态或确认取件完成 |
| 快递 | `DELETE /api/parcels/{id}` | 管理员删除未进入业务流程的快递 |
| 配送 | `POST /api/delivery-orders` | 用户为本人快递创建配送任务 |
| 配送 | `GET /api/delivery-orders/tasks` | 配送员分页查询可接及本人任务 |
| 配送 | `PUT /api/delivery-orders/{id}/accept` | 配送员并发安全接单 |
| 配送 | `PUT /api/delivery-orders/{id}/status` | 配送员更新本人任务公共状态 |
| 智能柜 | `POST /api/lockers/storage` | 包裹入柜 |
| 智能柜 | `POST /api/lockers/pickup` | 用户取件 |
| 智能柜 | `GET /api/lockers/{id}/status` | 柜机格口状态汇总 |
| 评价 | `POST /api/evaluations` | 提交配送评价 |
| 统计 | `GET /api/statistics/parcels/daily` | 每日快递数量 |
| 统计 | `GET /api/statistics/couriers/efficiency` | 配送员效率排行 |
| 统计 | `GET /api/statistics/pickups/popular-hours` | 热门取件时段 |

除注册、登录和 Swagger 外，`/api/**` 请求都需要提供 JWT；标记角色要求的接口还会校验 `USER`、`COURIER` 或 `ADMIN`：

```text
Authorization: Bearer <JWT>
```
