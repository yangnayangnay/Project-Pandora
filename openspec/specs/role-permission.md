# Role Permission

## Capability
5种角色层级（管理员/创始人/部门老总/团队长/员工）的权限分级。信息穿透鉴权。信息邀约请示机制。分层审核规则配置。用户注册和登录。

## Requirements
- 角色层级：ADMIN > FOUNDER > DEPT_HEAD > TEAM_LEADER > EMPLOYEE
- 注册接口支持用户名+密码+角色
- JWT无状态认证
- 不同角色查看不同范围的数据
- 创始人可穿透查看所有员工面板
- 部门老总/团队长可向下穿透查看下属面板
- 信息邀约请示机制

## Scenarios

### User registration
GIVEN 新用户填写用户名、密码和角色
WHEN 提交注册
THEN 账号创建成功，可使用该账号登录

### User login
GIVEN 已注册用户
WHEN 输入正确用户名和密码
THEN 登录成功，获取JWT token

### Founder穿透查看
GIVEN 创始人已登录
WHEN 查看任意员工面板
THEN 展示该员工完整工作信息