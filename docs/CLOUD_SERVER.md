# 云服务器使用指南（Pandora 后端）

> 本文档面向项目组开发成员，介绍 Pandora 后端所使用的阿里云 ECS 服务器情况、服务架构、部署与常用操作。所有命令均可直接复制执行。

## 1. 服务器概况

| 项 | 值 |
| --- | --- |
| 公网 IP | `123.56.153.138` |
| 云厂商/产品 | 阿里云 ECS（按量付费实例） |
| 规格 | 2 核 vCPU / 2 GiB 内存 |
| 操作系统 | Alibaba Cloud Linux 3.2104 LTS（RHEL 兼容系，包管理用 `dnf`/`yum`） |
| 用途 | Pandora 后端 API 服务 + Web 管理端静态托管 |

SSH 登录：`ssh root@123.56.153.138`（密钥方式，密钥由项目所有者配置；如无权限请联系项目所有者）。

## 2. 服务架构

| 服务 | 端口 | 管理方式 | 说明 |
| --- | --- | --- | --- |
| pandora-backend（Spring Boot） | 8080 | systemd（开机自启） | 后端 API，context-path 为 `/api`，健康检查 `GET http://123.56.153.138:8080/api/actuator/health` |
| nginx | 80 | systemd（开机自启） | Web 管理端静态托管 + `/api` 反向代理到 8080 |

后端安装目录 `/opt/pandora/`：

```
/opt/pandora/
├── app/                     # 后端 JAR（pandora-backend-1.0.0.jar）
├── data/                    # H2 数据库文件（pandora-data/pandora-db.*）
├── logs/                    # 后端日志（pandora-backend.log）
├── tools/                   # 运维工具（H2 在线查询用的 jar）
├── web/dist/                # Web 前端构建产物（nginx 托管根目录）
└── pandora.env              # 环境变量文件（API 鉴权密钥等，600 权限）
```

数据库为 **H2 文件库**（嵌入式，无独立数据库进程），数据随云盘持久，服务重启不丢数据。

## 3. 部署流程（后端更新）

在本地开发机完成代码修改后：

```bash
# 1. 本地构建（项目根目录）
cd pandora-backend
mvn clean package -DskipTests

# 2. 上传 JAR 到服务器
scp target/pandora-backend-1.0.0.jar root@123.56.153.138:/tmp/pandora-new.jar

# 3. 登录服务器替换并重启
ssh root@123.56.153.138
mv /tmp/pandora-new.jar /opt/pandora/app/pandora-backend-1.0.0.jar
systemctl restart pandora-backend

# 4. 验证服务恢复
curl http://123.56.153.138:8080/api/actuator/health
# 预期返回 {"status":"UP"}
```

Web 前端更新（如改动 pandora-web/）：

```bash
cd pandora-web
npm run build
scp -r dist/* root@123.56.153.138:/opt/pandora/web/dist/
# nginx 静态文件即时生效，无需重启
```

> 注意：后端重启会使所有已登录客户端的会话失效（会话存储在内存中），App 端会自动提示"会话已过期"并引导重新登录。

## 4. 测试账号

| 账号 | 角色 | 说明 |
| --- | --- | --- |
| admin0 | ADMIN | 管理员，可访问 Web 管理端全部功能 |
| founder1 | FOUNDER | 创始人 |
| dept_head2 | DEPT_HEAD | 部门负责人 |
| yjl_employee4 | EMPLOYEE | 员工 |

密码统一由**项目所有者私发**（不在本仓库中存放）。App 登录时 EMPLOYEE 角色须带 `clientType: APP` 参数。

## 5. API 鉴权现状

- 服务端部署了 API 密钥过滤器（`X-Api-Key` 请求头校验），当前处于**日志模式**（log）：未携带密钥的请求会被放行并记录日志，不影响现有客户端。
- 密钥存放在服务器 `/opt/pandora/pandora.env`（仅 root 可读）。如需密钥值，请向项目所有者索取。
- 登录/健康检查端点永久豁免（匿名可访问）。

## 6. 常用操作

```bash
# ---- 服务管理 ----
systemctl status pandora-backend    # 查看后端状态
systemctl restart pandora-backend   # 重启后端
systemctl stop pandora-backend      # 停止后端
journalctl -u pandora-backend -n 100 --no-pager   # 查看后端最近 100 行日志

# ---- H2 数据库在线查询（服务运行中可查，AUTO_SERVER 模式）----
ssh root@123.56.153.138 "java -cp /opt/pandora/tools/BOOT-INF/lib/h2-2.2.224.jar org.h2.tools.Shell \
  -url 'jdbc:h2:/opt/pandora/pandora-data/pandora-db;MODE=MySQL;AUTO_SERVER=TRUE' \
  -user sa -password '' \
  -sql \"SELECT COUNT(*) FROM users\""

# ---- nginx ----
nginx -t                              # 配置语法检查
systemctl reload nginx                # 重载配置
cat /etc/nginx/conf.d/pandora.conf    # 查看站点配置
```

## 7. Android App 连接云端

App 默认地址已内置为云服务器（`http://123.56.153.138:8080/api/`）。如需添加备用服务器或修改，在 App 内：**我的 → 设置 → 服务器地址**，支持多服务器列表与故障转移（断线自动切换、恢复自动回切）。

Web 管理端访问：浏览器打开 `http://123.56.153.138`（需在阿里云控制台安全组放行 TCP 80 端口）。

---

*文档更新时间：2026-10-23。如有疑问请联系项目所有者。*