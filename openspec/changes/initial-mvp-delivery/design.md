# Design: Initial MVP Delivery

## Architecture

### Android端 (app/)
- MVVM + Repository 架构
- Retrofit2 + RxJava3 网络层
- ViewBinding 数据绑定
- Navigation Component 导航
- Material Design 3 UI组件

### 后端 (pandora-backend/)
- Spring Boot 3 + Java 17
- JdbcTemplate 直接SQL（无JPA Entity）
- H2 内嵌数据库（MySQL兼容模式）
- JWT 无状态认证
- 内存Session（SessionStore，替代Redis）

### Web管理端 (pandora-web/)
- Vue3 + Vite + Element Plus
- Pinia 状态管理
- Axios HTTP客户端
- Vue Router 路由管理

## Key Decisions

### 数据库选择
- 开发环境使用 H2 内嵌数据库（MySQL兼容模式），文件存储在 D 盘
- Oracle profile 已预留，用户安装 Oracle 后可切换
- 建表脚本使用 CREATE IF NOT EXISTS，支持重复执行

### 认证模式
- 当前为调试模式：RoleAuthFilter 登录验证已注释，SecurityConfig permitAll
- 恢复方式：取消注释 RoleAuthFilter 和 SecurityConfig 中的权限分级代码

### 底部导航
- 5个tab：日程/视图/专注/其他/我的
- "其他"（原"统计"）包含：MBTI、关键词分析、AI建议、每日占卜、生日祝福