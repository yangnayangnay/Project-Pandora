# Project: Pandora（潘多拉）- 智能掌上工作系统

> 🎯 Nothing Is True, Everything Is Permitted.

## 项目简介

Project: Pandora 是一个智能掌上工作系统，旨在构建一个高效的任务分发、信息收集和数据分析系统，帮助公司管理层及员工实时掌握个人与团队工作情况。

系统包含：
- **Android 移动端 App**：主要面向员工使用，提供任务管理、日志记录、视图展示、AI分析等功能
- **Web 管理端**：主要面向管理层使用，提供全局看板、人员权限管理、AI分析查看等功能
- **后端服务**：提供 API 接口，处理业务逻辑、数据存储、AI接入等

## 技术栈

### Android 端
- 语言：Java
- 架构：MVVM + Repository
- 依赖注入：Hilt
- 网络：Retrofit 2 + OkHttp 3
- 本地存储：Room
- 响应式：RxJava 3
- 图表：MPAndroidChart
- 图片加载：Glide
- 加密存储：Security-Crypto

### 后端
- 框架：Spring Boot 3 + Java 17
- 数据库：H2（MySQL兼容模式，开发环境） / Oracle（生产环境，profile预留）
- 认证：JWT + 内存Session
- AI接入：本地兜底分析引擎（可配置外部 AI 服务，见 App 内 AI 配置）

### Web 管理端
- 框架：Vue 3 + Vite
- UI库：Element Plus
-5 状态管理：Pinia
- 图表：ECharts

## 三端目录结构

```
Project_Pandora/
├── app/                          # Android 移动端
│   └── src/main/java/com/example/project_pandora/
│       ├── core/                 # 核心基础架构（DI/网络/数据库/日志/崩溃/占卜）
│       ├── data/                 # 数据源层（远程/本地/偏好/同步）
│       ├── repository/           # Repository层（统一调度数据源）
│       ├── viewmodel/            # ViewModel层（MVVM）
│       ├── ui/8                  # UI层（界面/Fragment/Activity/Widget）
│       └── model/                # 数据模型（Entity/DTO/VO）
├── pandora-backend/              # 后端服务
│   └── src/main/java/com/pandora/backend/
│       ├── auth/                 # 认证鉴权
│       ├── user/                 # 用户管理
│       ├── task/                 # 任务管理
│       ├── log/                  # 日志管理
│       ├── view/                 # 视图聚合
│       ├── ai/                   # AI分析
│       ├── permission/           # 权限管理

│       ├── audit/                # 审计日志
│       ├── sync/                 # 数据同步
│       └── common/               # 通用组件
├── pandora-web/                  # Web 管理端
│   └── src/
│       ├── api/                  # 接口封装
│       ├── views/                # 页面视图
│       ├── components/           # 通用组件
│       ├── stores/               # Pinia状态管理
│       ├── router/               # 路由配置
│       └── utils/                # 工具函数
├── docs/                         # 文档
│   ├── CLOUD_SERVER.md           # 云服务器使用指南（组员必读）
│   └── architecture/             # 架构文档（UML/ER图）
├── openspec/                     # OpenSpec 规格驱动开发（见下）
└── README.md
```

## 启动方式

### 后端服务
```bash
# Windows 批处理启动
cd pandora-backend
run-server.bat

# 或使用 Maven（默认H2数据库，数据存储在项目目录下 pandora-data/）
mvn spring-boot:run

# 切换Oracle数据库（需先安装Oracle并配置连接信息）
mvn spring-boot:run -Dspring-boot.run.profiles=oracle
```

测试账号：admin0 / founder1 / dept_head2 等（密码由项目所有者私发，见 docs/CLOUD_SERVER.md）
H2 Console：http://localhost:8080/api/h2-console

### Android 端
```
使用 Android Studio 打开项目，点击 Run 按钮
```
App 默认连接云服务器（内置地址），也可在 我的→设置→服务器地址 中配置多服务器列表（支持故障转移）。

### Web 管理端
```bash
cd pandora-web
npm install
npm run dev          # 默认连云服务器（http://123.56.153.138:8080），无需本地后端
npm run dev:local    # 切回本地后端（自启 pandora-backend JAR）
```

## GitHub 仓库

https://github.com/yangnayangnay/Project-Pandora

## Sprint 规划

| Sprint | 时间 | 目标 | 状态 |
|--------|------|------|------|
| Sprint 0 | 第1-3周 | 需求分析与产品设计，建立工程基线 | ✅ 完成 |
| Sprint 1 | 第4-6周 | 首个MVP，完成首条核心用户链路 | ✅ 完成 |
| Sprint 2 | 第7-9周 | 核心功能与集成 | ✅ 完成 |
| Sprint 3 | 第10-14周 | 测试与验证 | ✅ 完成（真机+模拟器+云端三端验证） |
| Final Release | 第15-16周 | 验收与展示 | 进行中 |

## 核心功能

1. **任务管理**：创建、分发、接收、进度更新、确认完成，四象限管理
2. **日志管理**：录入（正文必填）、编辑、删除、查询、统计图表、十大重要工作编辑、日志模板保存与调用、当日已有日志自动跳转编辑
3. **视图展示**：月/周/日视图（月视图支持双指缩放）、导图页四大板块、任务面板（点击展开）
4. **AI分析**：MBTI性格测试、关键词分析、个性化建议、每日占卜、项目完成分析
5. **专注计时**：番茄钟工作法（25分钟专注+5分钟休息循环），周期统计
6. **角色权限**：5种角色层级(ADMIN/FOUNDER/DEPT_HEAD/TEAM_LEADER/EMPLOYEE)、登录鉴权、信息穿透、分层审核、人员调动
7. **通知系统**：应用内消息通道、通知偏好设置、后台轮询+前台服务保活（息屏可达）、多服务端故障转移
8. **多服务器支持**：服务端列表管理（≤5台）、自动故障转移与恢复回切、断连数据本地排队+中继同步
9. **主题模式**：深色/浅色双主题、跟随系统/始终白天/始终黑夜三态切换
10. **生日祝福**：设置生日、显示祝福与倒计时

## 底部导航

5个主页面（从左到右）：
1. **导图**：四象限视图（重要不紧急/重要且紧急/紧急不重要/不重要不紧急）
2. **视图**：月/周/日视图切换
3. **日志**：日志列表（查看过往/编辑/删除/模板）、统计、十大重要工作
4. **AI地图**：MBTI测试、关键词分析、AI建议、每日占卜、生日祝福、项目完成分析
5. **我的**：个人信息、专注模式（番茄钟）

## 非功能需求

- **NFR-1**：本地日志管理（定时定量清除、敏感信息过滤）
- **NFR-2**：可用性——深色模式全局适配（双套语义色板）、月视图双指缩放、通知息屏可达（前台服务保活）

## 云服务器部署

后端已部署至阿里云 ECS（`123.56.153.138`）：systemd 常驻 + nginx Web 托管，App 默认直连云端。**组员开发请先阅读 [docs/CLOUD_SERVER.md](docs/CLOUD_SERVER.md)**（部署流程/测试账号/常用运维操作）。

## 文档维护约定

- `docs/CLOUD_SERVER.md`：云服务器使用指南（组员交接必读）
- `docs/architecture/`：架构文档（UML 时序图/ER 图）
- `openspec/`：规格驱动开发变更记录

## 扩展功能

- **每日占卜**：基于梅花易数规则，以时间起卦，显示本卦/互卦/变卦及卦辞爻辞，用户手动起卦，纯本地计算- **生日祝福**：设置个人生日，当天显示祝福语，非生日显示倒计时

## OpenSpec

项目使用 OpenSpec 框架进行规格驱动开发（CLI v1.13.1）。

```bash
openspec list              # 列出所有变更
openspec list --specs      # 列出已交付能力规格
openspec view              # 交互式仪表盘
openspec validate <change> # 验证变更格式
```

目录结构：
```
openspec/
├── config.yaml             # 项目上下文配置
├── specs/                  # 已交付能力规格（6个）
└── changes/                # 变更记录
    └── initial-mvp-delivery/  # 初始MVP交付
```