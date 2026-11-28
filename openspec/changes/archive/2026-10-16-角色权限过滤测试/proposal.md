# Proposal: RoleAuthFilter 单元测试未规划记录

## 变更性质
本变更记录 initial-mvp-delivery 阶段 RoleAuthFilter 单元测试未规划，需明确标记为确实未做。

## 变更范围

### 未规划任务
**原任务（tasks.md 第144行）：**
- 规划 RoleAuthFilter 单元测试覆盖（白名单/无 Token/无效 Token/会话过期场景）

**实际状态：**
- 确实未做
- 无 RoleAuthFilter 单元测试文件

### 待测试场景
1. 白名单路径匿名访问
2. 无 Token 访问受保护资源
3. 无效 Token 访问受保护资源
4. Token 过期访问受保护资源
5. SessionStore 会话失效

### 原因分析
- 测试资源有限，优先覆盖 TaskStateMachine
- RoleAuthFilter 测试需模拟 Filter 链
- 建议后续 Sprint 补充

### Non-goals
- 不编写 RoleAuthFilter 单元测试
- 不修改 RoleAuthFilter 实现
- 不展望未来功能

## 覆盖模块
- pandora-backend (auth)

## Evidence

本变更记录的任务均未执行，无对应提交。

| 哈希 | 日期 | 主题 |
|------|------|------|
| 无 | - | 未执行 |

## 验收标准
- [x] 未规划任务已明确标记
- [x] 待测试场景已记录