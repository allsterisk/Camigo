# Camigo Vue3 前端设计与实现文档

## Phase 6 进度

- [x] Step 1：初始化 Vue 3 工程并配置 Router、Pinia、Element Plus、Axios
- [x] Step 2：登录认证体系
- [ ] Step 3：学生用户端
- [ ] Step 4：配送员端
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
