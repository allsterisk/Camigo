# Camigo Vue3 前端设计与实现文档

## Phase 6 进度

- [x] Step 1：初始化 Vue 3 工程并配置 Router、Pinia、Element Plus、Axios
- [x] Step 2：登录认证体系
- [x] Step 3：学生用户端
- [x] Step 4：配送员端
- [ ] Step 5：管理员后台
- [ ] Step 6：ECharts 数据大屏

## Step 1 架构

项目采用 Vue 3、Vite 与 TypeScript。`main.ts` 按 Pinia、Router、Element Plus 的顺序安装插件，确保路由守卫可安全读取用户仓库。路由使用 HTML5 History 模式和懒加载视图，通过 `meta.roles` 描述角色访问范围。Axios 实例集中处理基础地址、Bearer Token、401 会话清理和服务端错误提示。

## 目录职责

| 目录 | 职责 |
| --- | --- |
| `src/api` | 按业务领域封装后端接口，页面禁止直接使用 Axios |
| `src/assets` | 品牌图片、图标与全局样式 |
| `src/components` | 跨页面复用的展示组件 |
| `src/layout` | 角色端整体布局与导航框架 |
| `src/router` | 路由表、懒加载与访问控制 |
| `src/store` | Pinia 全局状态与会话状态 |
| `src/types` | API、路由和领域类型声明 |
| `src/utils` | Axios 等无界面基础设施 |
| `src/views` | 路由级薄视图，业务细节下沉到组件和组合式函数 |

## 品牌资源说明

当前附件作为深蓝色与白色视觉方向参考。原图包含大面积留白和生成水印，暂不直接作为生产资源；登录页与整体布局开发时应使用裁切后的无水印透明版本。

## Step 2 登录认证体系

### 组件边界

| 模块 | 职责 |
| --- | --- |
| `LoginView.vue` | 组合品牌面板和登录表单，不直接持有业务状态 |
| `LoginBrandPanel.vue` | 展示 Camigo 品牌与角色说明 |
| `LoginForm.vue` | 表单输入、Element Plus 校验和类型化提交事件 |
| `useLogin.ts` | 管理提交状态、错误提示和登录后的安全跳转 |
| `store/user.ts` | 登录请求、会话持久化、用户资料与退出登录 |
| `router/index.ts` | 公开路由、访客专属路由和角色访问控制 |

### 登录流程

1. 表单提交 `account` 与 `password` 到 `POST /api/users/login`。
2. Pinia 校验响应中的 Token 与角色，并将会话保存到 `localStorage`。
3. Axios 后续请求自动附加 `Authorization: Bearer <token>`。
4. Router 根据 USER、COURIER 或 ADMIN 跳转到相应入口；不合法的 `redirect` 参数会回退到角色首页。
5. 受保护请求返回 401 时，统一清除会话并跳回登录页。

### 安全约束

- 不在源码或环境示例中保存联调账号密码与 JWT。
- 仅允许已知角色进入受保护路由，损坏或旧版会话会在启动时自动清理。
- 页面只通过领域 API 模块访问后端，不直接调用 Axios。

## Step 3 学生用户端

### 页面与路由

| 路径 | 页面 | 主要能力 |
| --- | --- | --- |
| `/user` | 学生首页 | 快递总数、待取件数、配送中数量和最近快递 |
| `/user/parcels` | 我的快递 | 单号与状态筛选、分页列表、详情入口 |
| `/user/parcels/:id` | 快递详情 | 最新状态、标准配送进度、包裹信息和快递柜编号 |
| `/user/evaluation` | 配送评价 | 配送效率、服务体验和文字评价提交 |

学生端使用嵌套路由承载于 `UserLayout.vue`。列表、详情和评价分别由独立 composable 管理加载、错误和提交状态；页面组件仅负责组合展示组件。快递详情监听路由参数变化，因此从一个详情切换到另一个详情时会重新获取数据。

### 接口使用

- `GET /api/parcels/my`：首页统计、最近快递、筛选和分页。
- `GET /api/parcels/{id}`：读取当前用户有权查看的快递详情。
- `GET /api/delivery-orders/my?status=DELIVERED&evaluated=false`：读取当前学生已送达且尚未评价的配送订单。
- `POST /api/evaluations`：提交配送评分、服务评分和评价内容。

### 已知后端契约限制

1. 快递详情只返回位置编号和快递柜编号，没有位置名称、柜体名称或物流事件历史；页面明确展示编号，并将配送进度标记为标准流程而非真实事件时间线。
2. 若要完善上述体验，建议后端扩展快递详情 VO，返回位置展示信息和状态历史。

### 联调修复

学生端首次携带 Bearer Token 请求接口时，浏览器会发送 CORS OPTIONS 预检。原 `LoginInterceptor` 将无 JWT 的预检误判为未登录并返回 401，导致登录后所有业务请求表现为网络错误。Step 3 已让标准预检请求交由 Spring CORS 处理，真实 API 请求仍继续执行 JWT 与角色校验，并增加了拦截器回归测试。

评价页已完成待评价订单联调：由后端按 JWT 当前用户、公开状态和评价状态分页查询，前端使用配送单选择器展示快递单号、配送单号与配送员；提交成功后立即从待评价列表移除该订单，并覆盖加载失败、无数据和重试状态。

## Step 4 配送员端

### 页面与路由

| 路径 | 页面 | 主要能力 |
| --- | --- | --- |
| `/courier` | 配送员首页 | 今日任务、今日完成、配送效率、可接任务数量和最近任务 |
| `/courier/tasks` | 我的任务 | 可接任务与进行中任务筛选、分页、接单和确认送达 |
| `/courier/history` | 配送记录 | 分页查看本人已经送达的历史配送订单 |

配送员端使用 `CourierLayout.vue` 嵌套路由，并与学生端共同复用 `AppRoleLayout.vue`。共享布局只负责品牌导航、用户信息和退出登录；配送统计、任务表格和筛选分别由独立组件负责。任务筛选和分页写入 URL 查询参数，刷新页面或使用浏览器前进、后退时仍能恢复当前视图。

### 接口使用

- `GET /api/delivery-orders/tasks`：按 `WAITING`、`PROCESSING` 或 `DELIVERED` 查询配送员可见任务。
- `PUT /api/delivery-orders/{id}/accept`：配送员接取等待中的任务。
- `PUT /api/delivery-orders/{id}/status`：使用公开状态 `DELIVERED` 确认本人任务送达。

### 状态与统计口径

- Pinia 继续只保存跨页面登录会话，配送任务属于页面服务端状态，由 composable 管理，避免重复状态源。
- “今日任务”由当前进行中任务和今日已完成任务组成；“配送效率”为今日已完成数量占今日任务数量的百分比。
- 首页最多读取最近 200 条已完成任务用于当日统计，符合当前后端单页上限；正式运营数据量扩大后，建议新增配送员专属统计接口。
- 当前配送任务响应仅包含快递编号和目的地编号，因此任务表明确展示编号；若需展示快递单号、宿舍楼名称和详细地址，后端应扩展配送任务 VO。
