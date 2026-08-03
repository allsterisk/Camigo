# Camigo Vue3 前端设计与实现文档

## Phase 6 进度

- [x] Step 1：初始化 Vue 3 工程并配置 Router、Pinia、Element Plus、Axios
- [ ] Step 2：登录认证体系
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
