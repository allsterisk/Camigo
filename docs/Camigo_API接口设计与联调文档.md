# Camigo API 接口设计与联调文档

> 阶段：Phase 5 / Step 1 后端接口设计确认  
> 版本：v1.0-draft  
> 日期：2026-08-03  
> 状态：设计已确认；Step 2 用户模块与 Step 3 快递模块 API 已实施

## 1. 目标与范围

本文档定义 Camigo（校速达）Phase 5 的唯一 REST API 契约，覆盖用户注册登录、快递、配送、智能快递柜、评价、统计、JWT 认证、角色权限、Swagger 与 Vue3 联调约定。

本步骤只确认接口设计，不提前实现 Step 2～Step 5 的业务改造。

## 2. 已确认的设计原则

1. API 统一前缀为 `/api`，资源名使用复数和小写短横线。
2. 请求和响应均使用 UTF-8 JSON；时间使用 ISO-8601，例如 `2026-08-03T10:30:00+08:00`。
3. 除注册、登录、OpenAPI 和 Swagger UI 外，所有接口必须携带 JWT。
4. 认证头固定为 `Authorization: Bearer <token>`。
5. Controller 只负责参数接收、校验和响应封装；业务规则、事务和权限数据判断位于 Service。
6. DTO 与 VO 分离，不向前端暴露 Entity 或 MyBatis-Plus 对象。
7. 公共注册只允许创建 `USER`，不得通过 `role` 参数注册为 `COURIER` 或 `ADMIN`。
8. 权限继续采用 Spring Interceptor，在登录拦截基础上增加角色授权；JWT 只保存用户标识，角色从数据库或 Redis 读取，避免角色变更后旧 Token 长期越权。
9. Springdoc 使用 `@Tag`、`@Operation`、`@Parameter` 和 `@Schema`，不使用旧版 Swagger 2 的 `@ApiOperation`。
10. 所有写操作在 Service 层使用事务；状态变更使用条件更新或行锁，避免重复接单、重复入柜和重复取件。

## 3. 统一响应与 HTTP 状态

### 3.1 成功响应

HTTP 状态为 `200`，响应体：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

无数据的成功响应中 `data` 为 `null`。

### 3.2 分页响应

沿用现有 `PageResult<T>` 字段：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "pageSize": 20
  }
}
```

分页参数统一为 `page` 和 `pageSize`；`page >= 1`，`1 <= pageSize <= 200`。

### 3.3 错误响应

HTTP 状态和响应体 `code` 保持一致：

| HTTP/code | 含义 | 示例场景 |
|---:|---|---|
| 400 | 参数错误 | Validation 失败、非法状态流转 |
| 401 | 未登录 | Token 缺失、格式错误、过期 |
| 403 | 权限不足 | USER 访问管理员接口 |
| 404 | 资源不存在 | 快递、订单、快递柜不存在 |
| 409 | 资源冲突 | 重复单号、重复接单、重复取件 |
| 500 | 服务器异常 | 未预期异常；不向前端暴露数据库信息 |

```json
{
  "code": 400,
  "message": "手机号格式不正确",
  "data": null
}
```

`GlobalExceptionHandler` 统一处理 Validation、业务异常、资源不存在、无权限和未知异常。

## 4. JWT 与权限模型

### 4.1 角色

Phase 5 对外和数据库统一使用：

- `USER`：普通用户（学生）
- `COURIER`：配送员
- `ADMIN`：管理员

现有数据库角色 `STUDENT` 在 Step 2 迁移为 `USER`；`user_roles` 通过角色主键关联，不改变已有用户关系。

### 4.2 注册安全规则

`POST /api/users/register` 保留 `role` 参数以符合 Phase 5 契约，但公开注册时只能提交 `USER`。提交 `ADMIN` 或 `COURIER` 返回 403，管理员/配送员账号由管理员流程配置，防止自助提权。

### 4.3 路由权限矩阵

| 能力 | USER | COURIER | ADMIN |
|---|:---:|:---:|:---:|
| 查看/修改本人资料 | ✓ | ✓ | ✓ |
| 查询本人快递、创建配送任务、取件、评价 | ✓ |  | ✓（管理查询） |
| 查看配送任务、接单、更新配送状态、入柜 |  | ✓ | ✓ |
| 用户管理、快递录入/删除、统计 |  |  | ✓ |
| 查询快递柜及格口 | ✓ | ✓ | ✓ |

对象级权限必须在 Service 再校验，例如 USER 只能查询属于自己的快递，COURIER 只能更新本人接取的任务。

## 5. 用户模块 API

基础路径：`/api/users`

| 方法 | 路径 | 权限 | 请求 | 响应数据 |
|---|---|---|---|---|
| POST | `/api/users/register` | 公开 | `UserRegisterDTO` | `UserInfoVO` |
| POST | `/api/users/login` | 公开 | `UserLoginDTO` | `LoginVO` |
| GET | `/api/users/info` | 已登录 | 无 | `UserInfoVO` |
| PUT | `/api/users/{id}` | 本人或 ADMIN | `UserUpdateDTO` | `UserInfoVO` |

### 5.1 注册请求

```json
{
  "username": "student_001",
  "password": "Camigo@123",
  "phone": "13800138000",
  "role": "USER",
  "realName": "张三",
  "studentNo": "20260001"
}
```

校验：

- `username`：必填，4～50 位，只允许字母、数字、下划线。
- `password`：必填，8～64 位。
- `phone`：必填，中国大陆手机号格式。
- `role`：必填，公开注册只能为 `USER`。
- `realName`：必填，最多 50 位。
- `studentNo`：可选，最多 32 位。

### 5.2 登录请求与响应

请求：

```json
{
  "account": "student_001",
  "password": "Camigo@123"
}
```

`account` 支持用户名、手机号或学号。

响应 `data`：

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "user": {
    "id": 1,
    "username": "student_001",
    "realName": "张三",
    "phone": "13800138000",
    "studentNo": "20260001",
    "status": "ACTIVE",
    "roles": ["USER"]
  }
}
```

### 5.3 修改用户信息

普通用户只能修改本人 `realName`、`phone`、`studentNo`；管理员可额外修改 `status`，角色调整使用独立的管理员业务逻辑，禁止普通用户提交角色实现提权。

## 6. 快递模块 API

基础路径：`/api/parcels`

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| POST | `/api/parcels` | ADMIN | 录入快递 |
| GET | `/api/parcels` | ADMIN | 分页管理查询 |
| GET | `/api/parcels/my` | USER | 查询当前用户快递 |
| GET | `/api/parcels/{id}` | 所有人按对象权限 | 查询详情 |
| PUT | `/api/parcels/{id}/status` | USER（仅完成确认）/ADMIN/COURIER | 更新状态 |
| DELETE | `/api/parcels/{id}` | ADMIN | 删除尚未进入配送流程的快递 |

### 6.1 快递录入请求

```json
{
  "trackingNo": "SF202608030001",
  "carrierCode": "SF",
  "sender": "李四",
  "receiver": "张三",
  "phone": "13800138000",
  "lockerId": 1,
  "sizeClass": "M",
  "weightKg": 1.25
}
```

- `trackingNo` 全局唯一。
- `phone` 用于精确匹配收件用户，`receiver` 用于二次核验姓名。
- `lockerId` 为目标快递柜；Step 3 已为 `parcels` 增加可空的 `target_locker_id` 外键，避免目标柜信息丢失。
- 初始公共状态为 `ARRIVED`。

### 6.2 查询参数

`GET /api/parcels` 和 `/my` 支持：`page`、`pageSize`、`status`、`trackingNo`。

### 6.3 状态更新请求

```json
{
  "status": "DELIVERING",
  "remark": "配送员已取件"
}
```

公共状态机：

```text
ARRIVED -> DELIVERING -> STORED -> PICKED -> COMPLETED
```

快递入柜和用户取件必须由相应业务接口自动驱动状态，不允许前端绕过入柜/取件事务任意修改。Phase 4 已有的细粒度内部状态在 Step 3 统一映射到以上公共状态，历史日志保留。

USER 只能将本人已取件的快递从 `PICKED` 确认为 `COMPLETED`；`STORED` 与 `PICKED` 仍只允许智能柜业务事务产生。

## 7. 配送模块 API

基础路径：`/api/delivery-orders`

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| POST | `/api/delivery-orders` | USER | 为本人快递创建配送任务 |
| GET | `/api/delivery-orders/tasks` | COURIER | 分页查看可接/本人任务 |
| PUT | `/api/delivery-orders/{id}/accept` | COURIER | 接单 |
| PUT | `/api/delivery-orders/{id}/status` | COURIER | 更新本人任务状态 |

创建请求：

```json
{
  "parcelId": 1,
  "priority": "NORMAL",
  "predictedMinutes": 30
}
```

状态更新请求：

```json
{
  "status": "DELIVERED"
}
```

公共状态机：

```text
WAITING -> PROCESSING -> DELIVERED
```

现有细粒度配送状态继续用于内部流程，并通过 VO 映射为公共状态：

- `CREATED/ASSIGNING/ASSIGNED` → `WAITING`
- `ACCEPTED/PICKING_UP/IN_TRANSIT/ARRIVED` → `PROCESSING`
- `COMPLETED` → `DELIVERED`

接单必须使用行锁/条件更新保证同一订单只能由一个配送员接取。

## 8. 智能快递柜 API

基础路径：`/api/lockers`

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| GET | `/api/lockers` | 已登录 | 查询在线快递柜及可用格口数量 |
| GET | `/api/lockers/{id}/slots` | 已登录 | 查看格口状态 |
| POST | `/api/lockers/storage` | COURIER | 快递入柜 |
| POST | `/api/lockers/pickup` | USER | 用户取件 |

入柜事务必须同时完成：锁定格口、创建 `storage_record`、保存 BCrypt 取件码、更新格口状态和快递状态。

取件事务必须同时完成：校验所有权和取件码、锁定入柜记录、创建 `pickup_record`、释放格口并更新快递状态；重复请求不得重复取件。

## 9. 评价 API

基础路径：`/api/evaluations`

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| POST | `/api/evaluations` | USER | 评价本人已完成的配送服务 |
| GET | `/api/evaluations/{orderId}` | 订单相关用户/配送员或 ADMIN | 按配送订单查询评价 |

同一配送订单只能评价一次；评分范围为 1～5，内容最多 500 字。

## 10. 数据统计 API

基础路径：`/api/statistics`，全部仅限 `ADMIN`。

| 方法 | 路径 | 查询参数 | 返回 |
|---|---|---|---|
| GET | `/api/statistics/parcels` | `startDate`、`endDate` | 每日快递数量 |
| GET | `/api/statistics/delivery-ranking` | `limit`，默认 20 | 配送员效率排行 |
| GET | `/api/statistics/peak-time` | `days`、`limit` | 取件高峰时段 |

统计接口为只读查询，日期范围和数量上限必须校验，禁止拼接 SQL。

## 11. Swagger/OpenAPI 设计

接口按以下标签分组：

- `User API`
- `Parcel API`
- `Delivery API`
- `Locker API`
- `Evaluation API`
- `Statistics API`

每个接口提供：用途说明、权限说明、参数约束、成功响应和主要错误响应。OpenAPI 配置保留 Bearer JWT 安全方案；注册、登录接口显式标记为无需认证。

调试入口：`http://localhost:8080/swagger-ui/index.html`。

## 12. Vue3/Axios 联调约定

前端服务地址：`http://localhost:5173`；后端默认地址：`http://localhost:8080`。

后端 CORS 允许：

- Origin：`http://localhost:5173`
- Methods：`GET`、`POST`、`PUT`、`DELETE`、`OPTIONS`
- Headers：`Authorization`、`Content-Type`
- Credentials：当前 JWT 使用 Header，不依赖 Cookie，默认关闭

Axios 请求拦截器从 Pinia 持久化状态读取 Token 并添加 Bearer Header。响应拦截器按 HTTP 状态处理：401 清理登录状态并跳转登录页，403 展示权限不足，400/404/409 展示后端 `message`，500 展示统一系统错误。

## 13. 现状差异与实施清单

| 项目 | Phase 4 现状 | Phase 5 目标 | 实施步骤 |
|---|---|---|---|
| 用户路径 | `/api/user` | `/api/users` | Step 2 |
| 配送路径 | `/api/delivery/orders` | `/api/delivery-orders` | Step 4 |
| 状态更新 | ID 在请求体或 `/status` | `/{id}/status` | Step 3/4 |
| 成功业务码 | `0` | `200` | Step 2 先统一基础设施 |
| 错误业务码 | `40xxx/41xxx...` | HTTP 语义码 | Step 2 统一映射 |
| 普通用户角色 | `STUDENT` | `USER` | Step 2 数据迁移与代码调整 |
| 角色权限 | 仅验证 JWT | 路由角色 + 对象权限 | Step 2 起逐模块落地 |
| 用户修改 | 缺失 | `PUT /api/users/{id}` | Step 2 |
| 我的快递 | 通用 `/api/parcels` | `/api/parcels/my` | Step 3 |
| 目标快递柜 | 快递表未保存 | `target_locker_id` | Step 3 数据迁移 |
| 配送员任务列表 | 缺失 | `/tasks` | Step 4 |
| 快递柜列表/格口 | 仅 `/{id}/status` | `/` 与 `/{id}/slots` | 后续联调补齐 |
| 评价查询 | 缺失 | `/{orderId}` | 后续联调补齐 |
| 统计路径 | 三个旧路径 | Phase 5 三个目标路径 | 后续联调补齐 |
| Swagger 描述 | 仅全局配置 | Controller/DTO 完整注解 | 各模块同步完成 |
| CORS | 未配置 | 允许 localhost:5173 | Step 5 前完成 |

旧路径尚未对外发布，不保留重复兼容路由，避免 Swagger 和前端同时存在两套契约；相关自动化测试将随模块迁移更新。

## 14. 测试与验收流程

每个模块完成后至少执行：

1. DTO Validation 单元测试。
2. Service 正常、越权、非法状态和并发冲突测试。
3. Controller/OpenAPI 路径与 HTTP 状态集成测试。
4. 使用真实 MySQL 的只读 Mapper 冒烟测试。
5. Swagger 手工业务闭环：注册 → 登录 → 录入快递 → 查询 → 创建配送 → 接单 → 入柜/取件 → 评价 → 统计。
6. Maven 全量测试和可运行 JAR 打包。

## 15. Step 1 确认结论

后续开发以本文档的路径、响应码、角色和权限矩阵为准。确认后进入 Step 2，优先完成统一响应/认证基础改造，再实现用户注册、登录、用户信息和修改信息接口；完成后停止并等待下一次确认。

## 16. 实施进度

- [x] Step 1：后端接口设计确认
- [x] Step 2：统一响应码、用户注册/登录/信息/修改接口、USER 角色迁移
- [x] Step 3：快递模块 API、角色授权、目标快递柜关联与完成确认
- [ ] Step 4：配送模块 API
- [ ] Step 5：Vue3 Axios 联调
