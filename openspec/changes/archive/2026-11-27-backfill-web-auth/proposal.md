# Proposal: Web 端认证鉴权补录

## 变更性质
**事后补录** — 本变更记录 initial-mvp-delivery 阶段已实现但未纳入任务追踪的 Web 端认证鉴权功能。

## 变更范围

### 已实现功能
1. **Vue Router 路由守卫** — `pandora-web/src/router/index.js:176-195`
   - 认证守卫：`to.meta.requiresAuth !== false && !authenticated` 时重定向至登录页
   - 管理员守卫：`to.meta.adminOnly && role !== 'ADMIN'` 时重定向至用户页
   - 已认证用户访问登录页时重定向至首页
   - 已移除菜单路径重定向至个人中心

2. **Axios Token 拦截器** — `pandora-web/src/utils/request.js:23-32`
   - 请求拦截器：自动注入 `Authorization: Bearer ${token}`
   - 响应拦截器：401 时清除 Token 并重定向至登录页
   - 响应拦截器：403 时显示无操作权限提示
   - 响应拦截器：`x-user-role` 响应头变化时更新用户角色

### Non-goals
- 不新增功能
- 不修改现有代码
- 不展望未来功能

## 覆盖模块
- pandora-web (router, utils/request.js)

## 对应 Commit
- `4ef0ca1` docs: 附录B按16类277条重写
- `5d8a02b` docs: 测试报告口径修正为官方回归 25 类
- `7908417` feat(round19): Web菜单精简/权限收口

## 验收标准
- [x] 路由守卫已实现
- [x] Token 拦截器已实现
- [x] 401/403 处理已实现