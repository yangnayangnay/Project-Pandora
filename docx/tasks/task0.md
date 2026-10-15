# task0: 根据客户QA删除月相主题/晨星冒烟功能并移除邮件通知

## 背景
根据 `docx/软件开发与测试/问客户的问题.txt` 中客户确认：
1. 月相主题功能和晨星冒烟功能 → 删除
2. 通知方式不需要短信和邮件 → 仅保留应用内消息 + Android本地通知

## 任务步骤
1. [x] 清理 Android strings.xml 月相字符串残留（第114-124行）
2. [x] 删除 Web 端 ThemeConfigView.vue + api/theme.js，router /config 改为重定向到审计日志
3. [x] 删除后端 EmailService.java，清理 AuthController/ApprovalController 邮件调用、pom.xml mail依赖、application.yml 邮件配置
4. [x] 更新 openspec 文档（ui-theme.md/work-log-management.md/proposal.md/specs.md/config.yaml/tasks.md）移除月相/晨星冒烟
5. [x] 更新特色小巧思.txt 移除月相/晨星冒烟记录
6. [x] 首页对齐确认（MindMapFragment 四大板块已对齐 PDF 第五页，无需修改）
7. [ ] 更新需求改动.txt、代码改动.txt、README.md
8. [ ] 构建后端+Android 验证编译通过

## 验证标准
- 后端编译通过（无 EmailService 引用残留）
- Android 编译通过（无月相/晨星冒烟引用残留）
- Web 端启动正常（系统配置重定向到审计日志页）