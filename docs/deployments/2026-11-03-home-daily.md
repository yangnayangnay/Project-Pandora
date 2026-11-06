# 2026-11-03 首页与每日事项发布记录

用户要求将当前版本上传至既有服务器后，由 Codex 直接构建、上传并完成部署，未委派其他模型，未生成、复制或安装 APK。

- 服务器：`123.56.153.138`。
- 发布目录：`/opt/pandora/releases/20261103-home-daily`。
- 完成时间：2026-11-03 22:02:34（Asia/Shanghai）。
- 后端：`/opt/pandora/app/pandora-backend-1.0.0.jar`，Java 17 兼容；`pandora-backend` systemd 服务正常运行。
- Web：构建产物更新至 `/opt/pandora/web/dist`，保留旧版本带哈希资源以兼容已打开页面；nginx 正常运行。
- 备份：`/opt/pandora/backups/20261103-220156-home-daily`，包含旧 JAR、环境配置、Web 和停服后的 H2 数据库。

## 数据迁移

生产继续使用 H2 文件库 `/opt/pandora/pandora-data/pandora-db.mv.db`，保留原环境配置及 `SPRING_SQL_INIT_MODE=never`，未重放 schema 中的测试账号、数据更新或测试种子。

仅新增 `TASK_TAGS`、`TASK_TAG_LINKS`、`DAILY_ITEMS`、`LOG_COMMENTS` 四张表及对应索引。迁移前后对原有24张表逐行计算排序无关的内容摘要，并比较行数与摘要，全部一致。该校验针对停服迁移阶段；恢复服务后的正常业务写入不在此断言内。

## 验证

- 发布前后端112项、Android JVM 129项、Web 4项测试通过；Android源代码和测试源码已编译，未执行设备界面测试。
- 发布包已核对新接口类、Java 17字节码兼容性、H2依赖版本，并在上传后逐文件核对SHA-256。
- 服务器本地后端及nginx代理健康接口返回 `UP`；Web首页及首页引用的JS/CSS内容与发布产物一致。
- 公网 `http://123.56.153.138:8080/api/actuator/health` 返回HTTP 200及 `{"status":"UP"}`。
- 公网80端口的网页与代理API请求均超时，与2026-10-28发布记录一致。nginx监听 `0.0.0.0:80`，本机firewalld未运行且iptables INPUT策略为ACCEPT；应在云安全组或外层网络访问规则进一步核对TCP 80放行，本次未修改云安全组。
- 现有APK文件路径、大小和UTC修改时间对比，变化数0。

## 产物与恢复

- 后端SHA-256：`908044ea1a436055afaa80fa0ce336f4a51bcc3dede49efa32f4acfb0d8b3b9e`。
- Web ZIP SHA-256：`8404e09fe8ddddf7a5a5ca1b772e16786e59701caa8e29682fb1b1f32d5bac9b`。
- 服务器发布目录保存 `release-manifest.json`、`migration-h2.sql`、数据摘要、部署脚本及 `deployment-result.json`。
- 本地发布产物、构建日志和公网核对记录位于 `.build-temp/deploy-staging/20261103-home-daily/`。
- 本次迁移仅新增表。程序回退可恢复备份JAR和旧Web首页后重启服务，保留已产生的业务数据；完整数据库备份用于必要时人工核对恢复，不能在服务恢复后直接覆盖正常业务写入。

本次发布的是后端和Web。Android首页、公司导航及每日事项界面属于客户端代码，现有已安装App不会因服务器更新而自动获得新界面；遵照用户要求，本次未打包APK。
