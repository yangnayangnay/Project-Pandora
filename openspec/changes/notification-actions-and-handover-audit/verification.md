# Verification

日期：2026-11-27。变更：`notification-actions-and-handover-audit`。

- OpenSpec 2.2.0：读取项目配置，按 spec-driven 创建 proposal、两项能力规范、design、tasks；`validate notification-actions-and-handover-audit --strict` 通过。
- 后端：490项测试全部通过，包含批量已读的账号隔离、重复调用、空收件箱，以及在最大ID快照之后插入新通知仍保持未读的场景。
- Android：任务图确认无APK生成/安装；应用Java及资源编译、仪器测试源码编译、255项JVM测试通过。新增测试覆盖批量已读边界不会吞掉后续通知，旧响应不能触发已读消息提醒。
- Web：28项测试通过，生产构建通过；覆盖岗位和任务分别交接、零任务、历史大写字段、异常旧记录保留原文。
- Android新增通知页文案集中在 `res/values/notifications.xml`，顶部刷新与一键已读使用常驻菜单项。
- 未生成、复制或安装APK；未进行真机界面或息屏验收。手机界面变化需后续明确授权生成的新版本安装后才生效。

验证日志位于本地 `.build-temp/nov27-notification-actions-*.log` 和 `.build-temp/nov27-handover-view-*.log`。

云端隔离副本的实际HTTP验收通过：本人批量已读、其他账号未读不变、重复调用返回0项、批量操作后新到通知仍未读，以及已读数变化唤醒和管理员交接记录。隔离服务启动前后业务数据摘要一致。
正式发布目录为 `/opt/pandora/releases/20261127-notification-actions-and-handover-view`，更新前冷备保留在其 `cold-backup` 目录。
正式后端和管理员网页已更新，服务健康正常，更新前后业务表摘要未改变，账号密码及资料保持不变。
