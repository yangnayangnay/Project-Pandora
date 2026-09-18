4# Project: Pandora（潘多拉）- 智能掌上工作系统

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
- AI接入：deepseek API代理

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
│       ├── core/                 # 核心基础架构（DI/网络/数据库/日志/崩溃/主题/占卜/冒烟测试）
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
│       ├── theme/                # 主题管理
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
├── docx/                         # 文档
│   ├── 软件开发与测试/           # 源文档（只读）
│   └── docx_generate/            # 产出文档
│       ├── tasks/                # 任务执行方案
│       ├── 需求分析.txt
│       ├── 需求改动.txt
│       ├── 代码改动.txt
│       └── 特色小巧思.txt
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

默认管理员账号：admin / admin123
H2 Console：http://localhost:8080/api/h2-console

### Android 端
```
使用 Android Studio 打开项目，点击 Run 按钮
```

### Web 管理端
```bash
cd pandora-web
npm install
npm run dev
```

## GitHub 仓库

https://github.com/yangnayangnay/Project-Pandora

## Sprint 规划

| Sprint | 时间 | 目标 | 状态 |
|--------|------|------|------|
| Sprint 0 | 第1-3周 | 需求分析与产品设计，建立工程基线 | 进行中 |
| Sprint 1 | 第4-6周 | 首个MVP，完成首条核心用户链路 | 待开始 |
| Sprint 2 | 第7-9周 | 核心功能与集成 | 待开始 |
| Sprint 3 | 第10-14周 | 测试与验证 | 待开始 |
| Final Release | 第15-16周 | 验收与展示 | 待开始 |

## 核心功能

1. **任务管理**：创建、分发、接收、进度更新、确认完成，四象限管理
2. **日志管理**：录入、查询、统计图表、十大重要工作编辑
3. **视图展示**：月/周/日视图、导图页四大板块、信息数据地图
4. **AI分析**：MBTI性格测试、关键词分析、个性化建议、每日占卜
5. **专注计时**：番茄钟工作法（25分钟专注+5分钟休息循环），周期统计
6. **角色权限**：5种角色层级(ADMIN/FOUNDER/DEPT_HEAD/TEAM_LEADER/EMPLOYEE)、用户注册登录、信息穿透、邀约请示、分层审核
7. **主题管理**：8种月相主题自动切换、手动覆盖

## 底部导航

按需求文档设计为5个主页面：
1. **日程**：月/周/日视图切换，含日志查看入口
2. **视图**：导图页（四大板块业务展示）
3. **专注**：番茄钟计时器
4. **统计**：数据地图、AI分析、MBTI性格测试、每日占卜
5. **我的**：个人信息、主题设置

## 非功能需求

- **NFR-1**：月相主题自动切换（根据当天月相自动切换界面主题）
- **NFR-2**：本地日志管理（定时定量清除、敏感信息过滤）

## 文档维护约定

- `docx/软件开发与测试/`：源文档，只读不可修改
- `docx/docx_generate/`：产出文档
- `需求分析.txt`：项目需求分析文档
- `需求改动.txt`：需求变更记录
- `代码改动.txt`：代码变更记录
- `特色小巧思.txt`：特色实现细节记录
- `tasks/taskN.md`：任务执行方案
- `tasks/taskN_operation.md`：详细执行文档

## 扩展功能

- **每日占卜**：基于梅花易数规则，以时间起卦，显示本卦/互卦/变卦及卦辞爻辞，用户手动起卦，纯本地计算