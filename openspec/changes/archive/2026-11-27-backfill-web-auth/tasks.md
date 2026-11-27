# Tasks: Web 端认证鉴权补录

> 本变更记录 initial-mvp-delivery 阶段已实现但未纳入任务追踪的 Web 端认证鉴权功能。

## 1. Vue Router 路由守卫

- [x] 实现认证守卫（`pandora-web/src/router/index.js:186-188`，`to.meta.requiresAuth !== false && !authenticated` 时重定向至登录页）
- [x] 实现管理员守卫（`pandora-web/src/router/index.js:188-189`，`to.meta.adminOnly && role !== 'ADMIN'` 时重定向至用户页）
- [x] 实现已认证用户访问登录页重定向（`pandora-web/src/router/index.js:190-191`）
- [x] 实现已移除菜单路径重定向（`pandora-web/src/router/index.js:177-181`，`to.meta.removed` 时重定向至个人中心）

## 2. Axios Token 拦截器

- [x] 实现请求拦截器（`pandora-web/src/utils/request.js:23-32`，自动注入 `Authorization: Bearer ${token}`）
- [x] 实现 401 响应处理（`pandora-web/src/utils/request.js:59-63`，清除 Token 并重定向至登录页）
- [x] 实现 403 响应处理（`pandora-web/src/utils/request.js:64-65`，显示无操作权限提示）
- [x] 实现角色更新处理（`pandora-web/src/utils/request.js:39-45`，`x-user-role` 响应头变化时更新用户角色）

## 3. 验收

- [x] 路由守卫已实现（commit `7908417`）
- [x] Token 拦截器已实现（commit `7908417`）
- [x] 401/403 处理已实现（commit `7908417`）