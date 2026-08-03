# Camigo Web

Camigo（校速达）Vue 3 前端联调项目，使用 Vite、Element Plus、Axios、Vue Router 和 Pinia。

## 环境变量

复制 `.env.example` 为 `.env.local`，按需修改：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## 启动

```shell
npm install
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`。后端默认运行于 `http://localhost:8080`。

## Axios 联调规则

- 请求自动读取本地会话并添加 `Authorization: Bearer <token>`。
- 成功响应自动解包后端统一的 `Result<T>.data`。
- 400、403、404、409、500 使用 Element Plus 统一提示。
- 401 自动清除会话并跳转登录页，保留原目标路径。
