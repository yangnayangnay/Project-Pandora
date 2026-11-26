================================================================================
Project Pandora 结构化测试用例清单（供AI生成测试代码）
================================================================================
生成日期：2026-11-22
说明：本清单包含输入参数、期望输出、场景标注，可直接传给AI生成测试用例
格式：语义化命名 '模块-操作-输入特征-预期动作'

================================================================================
一、人员创建模块（AccountCreationService）- 高优先级
================================================================================

【函数签名】
public Map<String, Object> create(Map<String, Object> request)

【输入参数结构】
request = {
    "employeeNo": "string, 工号, 1-64位字母数字点下划线短横线",
    "password": "string, 密码, 至少6位，UTF-8不超过72字节",
    "role": "string, 角色: FOUNDER|DEPT_HEAD|TEAM_LEADER|EMPLOYEE",
    "realName": "string, 真实姓名, 最多64字",
    "email": "string, 邮箱, 符合正则格式",
    "phone": "string, 手机号, 11位1开头",
    "idCard": "string, 身份证号, 18位",
    "birthday": "string, 生日, YYYY-MM-DD格式",
    "departmentId": "long, 部门ID（可选，根据角色决定是否需要）",
    "teamId": "long, 团队ID（可选，根据角色决定是否需要）"
}

【输出结构】
成功 = {"id": createdId, "employeeNo": employeeNo, "username": employeeNo}
异常 = BusinessException(ErrorCode, message)

--------------------------------------------------------------------------------
测试用例骨架 - 人员创建（同类/边界/异类）
--------------------------------------------------------------------------------

[同类] 人员创建-正常创建员工-预期成功
  输入：employeeNo="EMP001", password="Pass123", role="EMPLOYEE", 
        realName="张三", email="zhangsan@test.com", phone="13800138000",
        idCard="110101199001011234", birthday="1990-01-01",
        departmentId=1, teamId=1
  期望输出：id=新ID, employeeNo="EMP001", username="EMP001"
  数据库验证：users表中新增记录，approval_status='APPROVED', deleted=0

[边界] 人员创建-工号最小长度1-预期成功
  输入：employeeNo="A", password="Pass123", role="EMPLOYEE", ...
  期望输出：创建成功

[边界] 人员创建-工号最大长度64-预期成功
  输入：employeeNo="a".repeat(64), password="Pass123", role="EMPLOYEE", ...
  期望输出：创建成功

[边界] 人员创建-密码最小长度6-预期成功
  输入：employeeNo="EMP002", password="123456", role="EMPLOYEE", ...
  期望输出：创建成功

[边界] 人员创建-密码最大UTF-8长度72-预期成功
  输入：employeeNo="EMP003", password="a".repeat(72), role="EMPLOYEE", ...
  期望输出：创建成功

[边界] 人员创建-真实姓名最大长度64-预期成功
  输入：realName="张".repeat(64), ...
  期望输出：创建成功

[异类-异常] 人员创建-工号为空-预期拒绝
  输入：employeeNo="", password="Pass123", role="EMPLOYEE", ...
  期望输出：ErrorCode.MISSING_REQUIRED_FIELD, "工号不能为空"

[异类-异常] 人员创建-工号含特殊字符-预期拒绝
  输入：employeeNo="EMP@001", password="Pass123", role="EMPLOYEE", ...
  期望输出：ErrorCode.BAD_REQUEST, "工号须为1至64位字母、数字、点、下划线或短横线"

[异类-异常] 人员创建-工号与username不一致-预期拒绝
  输入：employeeNo="EMP001", username="EMP002", password="Pass123", ...
  期望输出：ErrorCode.BAD_REQUEST, "工号与兼容账号字段不一致"

[异类-异常] 人员创建-密码过短-预期拒绝
  输入：employeeNo="EMP001", password="123", role="EMPLOYEE", ...
  期望输出：ErrorCode.PASSWORD_TOO_SHORT

[异类-异常] 人员创建-密码超长UTF-8-预期拒绝
  输入：employeeNo="EMP001", password="a".repeat(73), role="EMPLOYEE", ...
  期望输出：ErrorCode.BAD_REQUEST, "密码不能超过72个UTF-8字节"

[异类-异常] 人员创建-角色无效-预期拒绝
  输入：employeeNo="EMP001", password="Pass123", role="INVALID", ...
  期望输出：ErrorCode.INVALID_ROLE

[异类-异常] 人员创建-角色为ADMIN-预期拒绝
  输入：employeeNo="EMP001", password="Pass123", role="ADMIN", ...
  期望输出：ErrorCode.INVALID_ROLE

[异类-异常] 人员创建-真实姓名为空-预期拒绝
  输入：realName="", ...
  期望输出：ErrorCode.MISSING_REQUIRED_FIELD, "真实姓名不能为空"

[异类-异常] 人员创建-真实姓名超长-预期拒绝
  输入：realName="张".repeat(65), ...
  期望输出：ErrorCode.BAD_REQUEST, "真实姓名不能超过64字"

[异类-异常] 人员创建-邮箱格式错误-预期拒绝
  输入：email="invalid-email", ...
  期望输出：ErrorCode.INVALID_EMAIL_FORMAT

[异类-异常] 人员创建-邮箱超长-预期拒绝
  输入：email="a".repeat(129) + "@test.com", ...
  期望输出：ErrorCode.INVALID_EMAIL_FORMAT

[异类-异常] 人员创建-手机号格式错误-预期拒绝
  输入：phone="1234567890", ...
  期望输出：ErrorCode.INVALID_PHONE_FORMAT

[异类-异常] 人员创建-身份证格式错误-预期拒绝
  输入：idCard="123456789012345678", ...
  期望输出：ErrorCode.INVALID_ID_CARD

[异类-异常] 人员创建-生日格式错误-预期拒绝
  输入：birthday="1990/01/01", ...
  期望输出：ErrorCode.BAD_REQUEST, "生日须为有效的年月日"

[异类-异常] 人员创建-生日晚于今天-预期拒绝
  输入：birthday="2099-12-31", ...
  期望输出：ErrorCode.BAD_REQUEST, "生日不能晚于今天"

[异类-异常] 人员创建-部门不存在-预期拒绝
  输入：departmentId=99999, ...
  期望输出：ErrorCode.BAD_REQUEST, "所选部门不存在或已停用"

[异类-异常] 人员创建-团队不属于部门-预期拒绝
  输入：departmentId=1, teamId=999, ...
  期望输出：ErrorCode.BAD_REQUEST, "所选团队不存在、已停用或不属于所选部门"

[异类-异常] 人员创建-工号已存在-预期拒绝
  输入：employeeNo="EXISTING", ... (数据库中已存在)
  期望输出：ErrorCode.USERNAME_ALREADY_EXISTS

[异类-异常] 人员创建-邮箱已存在-预期拒绝
  输入：email="existing@test.com", ... (数据库中已存在)
  期望输出：ErrorCode.EMAIL_ALREADY_EXISTS

[异类-异常] 人员创建-身份证已存在-预期拒绝
  输入：idCard="110101199001011234", ... (数据库中已存在)
  期望输出：ErrorCode.ID_CARD_ALREADY_EXISTS

[异类-异常] 人员创建-缺少部门ID（需要部门的角色）-预期拒绝
  输入：role="DEPT_HEAD", departmentId=null, ...
  期望输出：ErrorCode.MISSING_REQUIRED_FIELD, "请选择部门"

[异类-异常] 人员创建-缺少团队ID（需要团队的角色）-预期拒绝
  输入：role="TEAM_LEADER", teamId=null, ...
  期望输出：ErrorCode.MISSING_REQUIRED_FIELD, "请选择团队"


================================================================================
二、人员状态变更模块（UserController.updateUserOrg / PersonnelController.employment）- 高优先级
================================================================================

【函数签名 - UserController.updateUserOrg】
public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserOrg(
    @PathVariable Long userId, @RequestBody Map<String, Object> request)

【输入参数】
userId: 路径参数，用户ID
request = {
    "departmentId": "long, 部门ID（可选）",
    "teamId": "long, 团队ID（可选）"
}

【输出结构】
成功 = {"userId": userId, "departmentId": departmentId, "teamId": teamId}
异常 = BusinessException(ErrorCode, message)

【权限要求】仅 ADMIN 角色可操作

--------------------------------------------------------------------------------
测试用例骨架 - 人员归属调动
--------------------------------------------------------------------------------

[同类] 人员调动-员工换部门换团队-预期成功
  前置：用户ID=100, role="EMPLOYEE", 当前departmentId=1, teamId=1
  输入：userId=100, request={"departmentId": 2, "teamId": 3}
  期望输出：userId=100, departmentId=2, teamId=3
  数据库验证：users表中department_id=2, team_id=3

[同类] 人员调动-部门负责人只换部门-预期成功
  前置：用户ID=101, role="DEPT_HEAD", 当前departmentId=1
  输入：userId=101, request={"departmentId": 2}
  期望输出：userId=101, departmentId=2, teamId=null
  数据库验证：department_id=2, team_id=null

[边界] 人员调动-创始人调动-预期成功（不保留团队）
  前置：用户ID=102, role="FOUNDER", 当前departmentId=1, teamId=1
  输入：userId=102, request={}
  期望输出：userId=102, departmentId=null, teamId=null

[异类-越权] 人员调动-非ADMIN操作-预期拒绝
  前置：当前用户角色="EMPLOYEE"
  输入：userId=100, request={"departmentId": 2}
  期望输出：HttpStatus.FORBIDDEN, "无权操作"

[异类-异常] 人员调动-用户不存在-预期拒绝
  输入：userId=99999, request={"departmentId": 2}
  期望输出：ErrorCode.USER_NOT_FOUND

[异类-异常] 人员调动-管理员账号调动-预期拒绝
  前置：用户ID=103, role="ADMIN"
  输入：userId=103, request={"departmentId": 2}
  期望输出：ErrorCode.BAD_REQUEST, "管理员账号不参与公司岗位调整"

[异类-异常] 人员调动-部门ID无效-预期拒绝
  输入：userId=100, request={"departmentId": -1}
  期望输出：ErrorCode.BAD_REQUEST, "必须选择有效部门"

[异类-异常] 人员调动-团队与部门不匹配-预期拒绝
  输入：userId=100, request={"departmentId": 1, "teamId": 999}
  期望输出：ErrorCode.BAD_REQUEST, "团队与部门不匹配"

[异类-异常] 人员调动-员工缺少团队-预期拒绝
  前置：用户ID=100, role="EMPLOYEE"
  输入：userId=100, request={"departmentId": 1}
  期望输出：ErrorCode.BAD_REQUEST, "请为团队负责人或员工选择团队"


【函数签名 - PersonnelController.employment】
public ApiResponse<Void> employment(
    Authentication auth, @PathVariable long id, @RequestBody Map<String, Object> body)

【输入参数】
id: 路径参数，用户ID
body = {
    "role": "string, 新角色",
    "departmentId": "long, 部门ID（可选）",
    "teamId": "long, 团队ID（可选）",
    "superiorId": "long, 上级ID（可选，已弃用）"
}

【输出结构】
成功 = ApiResponse.success()
异常 = BusinessException(ErrorCode, message)

【权限要求】仅 ADMIN 角色可操作

--------------------------------------------------------------------------------
测试用例骨架 - 人员入职/状态变更
--------------------------------------------------------------------------------

[同类] 人员入职-员工入职分配部门团队-预期成功
  前置：用户ID=200, role=null（未入职状态）
  输入：id=200, body={"role": "EMPLOYEE", "departmentId": 1, "teamId": 1}
  期望输出：success
  数据库验证：role="EMPLOYEE", department_id=1, team_id=1

[同类] 人员状态变更-员工晋升为团队负责人-预期成功
  前置：用户ID=201, role="EMPLOYEE"
  输入：id=201, body={"role": "TEAM_LEADER", "departmentId": 1, "teamId": 2}
  期望输出：success
  数据库验证：role="TEAM_LEADER", team_id=2

[同类] 人员状态变更-团队负责人降为员工-预期成功
  前置：用户ID=202, role="TEAM_LEADER"
  输入：id=202, body={"role": "EMPLOYEE", "departmentId": 1, "teamId": 2}
  期望输出：success

[同类] 人员状态变更-员工跨部门调动-预期成功
  前置：用户ID=203, role="EMPLOYEE", departmentId=1, teamId=1
  输入：id=203, body={"role": "EMPLOYEE", "departmentId": 2, "teamId": 3}
  期望输出：success

[异类-越权] 人员入职-非ADMIN操作-预期拒绝
  前置：当前用户角色="DEPT_HEAD"
  输入：id=200, body={"role": "EMPLOYEE", ...}
  期望输出：HttpStatus.FORBIDDEN

[异类-异常] 人员入职-用户不存在-预期拒绝
  输入：id=99999, body={"role": "EMPLOYEE", ...}
  期望输出：ErrorCode.USER_NOT_FOUND

[异类-异常] 人员入职-管理员账号变更-预期拒绝
  前置：用户ID=204, role="ADMIN"
  输入：id=204, body={"role": "EMPLOYEE", ...}
  期望输出：ErrorCode.BAD_REQUEST, "管理员账号不参与公司岗位调整"

[异类-异常] 人员入职-角色无效-预期拒绝
  输入：id=200, body={"role": "INVALID", ...}
  期望输出：ErrorCode.BAD_REQUEST, "请选择有效职级"

[异类-异常] 人员入职-缺少必填字段-预期拒绝
  输入：id=200, body={"role": "EMPLOYEE"}
  期望输出：ErrorCode.BAD_REQUEST, "不支持的人员调整字段"

[异类-异常] 人员入职-缺少部门（需要部门的角色）-预期拒绝
  输入：id=200, body={"role": "DEPT_HEAD"}
  期望输出：ErrorCode.BAD_REQUEST, "必须为人员分配部门"

[异类-异常] 人员入职-缺少团队（需要团队的角色）-预期拒绝
  输入：id=200, body={"role": "EMPLOYEE", "departmentId": 1}
  期望输出：ErrorCode.BAD_REQUEST, "请为团队负责人或员工选择团队"

[异类-异常] 人员入职-部门不存在-预期拒绝
  输入：id=200, body={"role": "EMPLOYEE", "departmentId": 999, "teamId": 1}
  期望输出：ErrorCode.BAD_REQUEST, "部门不存在"

[异类-异常] 人员入职-团队与部门不匹配-预期拒绝
  输入：id=200, body={"role": "EMPLOYEE", "departmentId": 1, "teamId": 999}
  期望输出：ErrorCode.BAD_REQUEST, "团队与部门不匹配"


================================================================================
三、部门/团队管理模块（PersonnelController）- 中优先级
================================================================================

【函数签名 - createDepartment】
public ApiResponse<Map<String, Object>> createDepartment(@RequestBody Map<String, Object> body)

【输入参数】
body = {"name": "string, 部门名称, 1-128字符"}

【输出结构】
成功 = {"id": newId, "name": name}
异常 = BusinessException(ErrorCode, message)

【权限要求】仅 ADMIN 角色可操作

--------------------------------------------------------------------------------
测试用例骨架 - 部门管理
--------------------------------------------------------------------------------

[同类] 部门创建-正常名称-预期成功
  输入：body={"name": "研发部"}
  期望输出：id=新ID, name="研发部"
  数据库验证：departments表中新增记录，deleted=0

[边界] 部门创建-名称最小长度1-预期成功
  输入：body={"name": "A"}
  期望输出：创建成功

[边界] 部门创建-名称最大长度128-预期成功
  输入：body={"name": "测".repeat(128)}
  期望输出：创建成功

[异类-异常] 部门创建-名称为空-预期拒绝
  输入：body={"name": ""}
  期望输出：ErrorCode.BAD_REQUEST, "部门名称长度应为1至128个字符"

[异类-异常] 部门创建-名称超长-预期拒绝
  输入：body={"name": "测".repeat(129)}
  期望输出：ErrorCode.BAD_REQUEST, "部门名称长度应为1至128个字符"

[异类-异常] 部门创建-名称重复-预期拒绝
  前置：已存在部门"研发部"
  输入：body={"name": "研发部"}
  期望输出：ErrorCode.BAD_REQUEST, "部门名称已存在"

[异类-越权] 部门创建-非ADMIN操作-预期拒绝
  前置：当前用户角色="DEPT_HEAD"
  输入：body={"name": "新部门"}
  期望输出：HttpStatus.FORBIDDEN


【函数签名 - updateDepartment】
public ApiResponse<Void> updateDepartment(@PathVariable long id, @RequestBody Map<String, Object> body)

[同类] 部门更新-修改名称-预期成功
  前置：部门ID=1, name="旧名称"
  输入：id=1, body={"name": "新名称"}
  期望输出：success
  数据库验证：name="新名称"

[异类-异常] 部门更新-部门不存在-预期拒绝
  输入：id=99999, body={"name": "新名称"}
  期望输出：ErrorCode.NOT_FOUND, "部门不存在"

[异类-异常] 部门更新-名称重复-预期拒绝
  前置：已存在部门"研发部"和"市场部"
  输入：id=1, body={"name": "市场部"}
  期望输出：ErrorCode.BAD_REQUEST, "部门名称已存在"


【函数签名 - deleteDepartment】
public ApiResponse<Void> deleteDepartment(@PathVariable long id)

[同类] 部门删除-空部门-预期成功
  前置：部门ID=99, 无关联人员或团队
  输入：id=99
  期望输出：success
  数据库验证：deleted=1

[异类-异常] 部门删除-部门不存在-预期拒绝
  输入：id=99999
  期望输出：ErrorCode.NOT_FOUND, "部门不存在"

[异类-异常] 部门删除-部门仍有人员-预期拒绝
  前置：部门ID=1, 有员工关联
  输入：id=1
  期望输出：ErrorCode.BAD_REQUEST, "部门仍有人员或团队，无法删除"

[异类-异常] 部门删除-部门仍有团队-预期拒绝
  前置：部门ID=1, 有团队关联
  输入：id=1
  期望输出：ErrorCode.BAD_REQUEST, "部门仍有人员或团队，无法删除"


【函数签名 - createTeam / updateTeam / deleteTeam】
（类似部门管理，结构相同）

[同类] 团队创建-正常创建-预期成功
  输入：body={"name": "前端组", "departmentId": 1}
  期望输出：id=新ID, name="前端组", department_id=1

[异类-异常] 团队创建-部门不存在-预期拒绝
  输入：body={"name": "前端组", "departmentId": 999}
  期望输出：ErrorCode.BAD_REQUEST, "所属部门不存在"

[异类-异常] 团队创建-同部门同名团队已存在-预期拒绝
  前置：部门1下已存在"前端组"
  输入：body={"name": "前端组", "departmentId": 1}
  期望输出：ErrorCode.BAD_REQUEST, "该部门已存在同名团队"

[异类-异常] 团队删除-团队仍有成员-预期拒绝
  前置：团队ID=1, 有TEAM_LEADER或EMPLOYEE关联
  输入：id=1
  期望输出：ErrorCode.BAD_REQUEST, "团队仍有成员，无法删除"


================================================================================
四、用户管理模块（UserController）- 中优先级
================================================================================

【函数签名 - getCurrentUser】
@GetMapping("/me")
public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser()

【输出结构】
成功 = {
    "id": userId,
    "username": username,
    "role": role,
    "email": email,
    "realName": realName,
    "phone": phone,
    "departmentId": departmentId,
    "teamId": teamId,
    "departmentName": departmentName,
    "teamName": teamName,
    "avatar": avatar,
    "signature": signature,
    "themeMode": themeMode,
    "manualTheme": manualTheme
}

[同类] 获取当前用户-正常获取-预期成功
  前置：当前登录用户ID=1
  输入：无
  期望输出：包含用户完整信息

[异类-异常] 获取当前用户-用户不存在-预期拒绝
  前置：用户ID不存在于数据库
  输入：无
  期望输出：ErrorCode.USER_NOT_FOUND


【函数签名 - listUsers】
@GetMapping("/list")
public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listUsers(
    @RequestParam(required = false) String role,
    @RequestParam(required = false) Long departmentId,
    @RequestParam(required = false) Long teamId)

[同类] 获取用户列表-无过滤条件-预期成功
  输入：无参数
  期望输出：用户列表（排除ADMIN角色，非ADMIN用户）

[同类] 获取用户列表-按角色过滤-预期成功
  输入：role="EMPLOYEE"
  期望输出：仅EMPLOYEE角色用户

[同类] 获取用户列表-按部门过滤-预期成功
  输入：departmentId=1
  期望输出：该部门下所有用户

[异类-异常] 获取用户列表-部门不存在-预期成功（返回空）
  输入：departmentId=99999
  期望输出：空列表


================================================================================
五、测试用例元数据
================================================================================

【输入类别定义】
- 同类：正常输入，符合业务规则
- 边界：输入在边界值（最小/最大长度、最小/最大值）
- 异类-异常：输入违反业务规则，预期抛出业务异常
- 异类-越权：权限不足，预期返回403

【优先级定义】
- 高优先级：核心业务功能（人员创建、状态变更）
- 中优先级：重要业务功能（部门管理、用户管理）
- 低优先级：辅助功能（配置、日志）

【数据库验证点】
- 验证插入/更新后的数据正确性
- 验证deleted字段状态
- 验证关联表数据一致性
- 验证审批状态

================================================================================
六、组织架构层级模块（HierarchyService）- 高优先级
================================================================================

【函数签名】
public List<Map<String, Object>> getSubordinates(Long userId)

【输出结构】
List<Map<String, Object>> = [
    {"id": userId, "username": username, "realName": realName, "role": role, "depth": depth}
]

--------------------------------------------------------------------------------
测试用例骨架 - 组织架构层级
--------------------------------------------------------------------------------

[同类] 获取下属-ADMIN获取所有用户-预期成功
  前置：当前用户ID=1, role="ADMIN"
  输入：userId=1
  期望输出：所有非ADMIN用户（排除自己）

[同类] 获取下属-FOUNDER获取所有非ADMIN用户-预期成功
  前置：当前用户ID=2, role="FOUNDER"
  输入：userId=2
  期望输出：所有非ADMIN非FOUNDER用户

[同类] 获取下属-普通用户获取直属下属-预期成功
  前置：当前用户ID=3, role="DEPT_HEAD"
  输入：userId=3
  期望输出：该用户的所有下属（递归到maxDepth）

[边界] 获取下属-达到最大深度限制-预期截断
  前置：层级深度=10（maxDepth=10）
  输入：userId=1
  期望输出：仅返回深度<=10的下属

[异类-异常] 获取下属-用户不存在-预期返回空
  输入：userId=99999
  期望输出：空列表


【函数签名】
public boolean isSubordinate(Long superiorId, Long subordinateId)

[同类] 判断下属-直接下属-预期true
  输入：superiorId=1, subordinateId=2（2是1的直接下属）
  期望输出：true

[同类] 判断下属-间接下属-预期true
  输入：superiorId=1, subordinateId=3（3是2的下属，2是1的下属）
  期望输出：true

[异类-异常] 判断下属-自己不是自己的下属-预期false
  输入：superiorId=1, subordinateId=1
  期望输出：false

[异类-异常] 判断下属-非下属关系-预期false
  输入：superiorId=1, subordinateId=100（无关系）
  期望输出：false


【函数签名】
public Map<String, Object> addRelation(Long superiorId, Long subordinateId, boolean forceReplace)

[同类] 添加上下级关系-正常添加-预期成功
  输入：superiorId=1, subordinateId=2, forceReplace=false
  期望输出：{"success": true}
  数据库验证：user_hierarchy中新增记录

[异类-异常] 添加上下级关系-形成循环-预期拒绝
  前置：2是1的下属
  输入：superiorId=2, subordinateId=1
  期望输出：{"success": false, "reason": "不能形成循环关系"}

[异类-异常] 添加上下级关系-已有上级且未强制替换-预期拒绝
  前置：2已有上级3
  输入：superiorId=1, subordinateId=2, forceReplace=false
  期望输出：{"success": false, "reason": "该用户已有直接上级，需确认替换"}

[同类] 添加上下级关系-已有上级但强制替换-预期成功
  前置：2已有上级3
  输入：superiorId=1, subordinateId=2, forceReplace=true
  期望输出：{"success": true}
  数据库验证：原关系被删除，新关系已建立


================================================================================
七、用户资料管理模块（UserController）- 中优先级
================================================================================

【函数签名 - updateProfile】
@PutMapping("/me")
public ResponseEntity<ApiResponse<Void>> updateProfile(@RequestBody Map<String, Object> request)

【输入参数】
request = {
    "avatar": "string, 头像URL（可选）",
    "signature": "string, 个人签名（可选，最多20字，最多4行）",
    "email": "string, 邮箱（可选，仅ADMIN）",
    "phone": "string, 手机号（可选，仅ADMIN）"
}

【权限要求】
- 普通用户：仅可修改avatar和signature
- ADMIN：可修改email和phone
- 不可修改username和realName

--------------------------------------------------------------------------------
测试用例骨架 - 用户资料更新
--------------------------------------------------------------------------------

[同类] 更新个人资料-普通用户修改头像-预期成功
  前置：当前用户ID=1, role="EMPLOYEE"
  输入：request={"avatar": "http://example.com/avatar.jpg"}
  期望输出：success
  数据库验证：avatar字段已更新

[同类] 更新个人资料-普通用户修改签名-预期成功
  输入：request={"signature": "这是我的签名"}
  期望输出：success

[边界] 更新个人资料-签名最大长度20字-预期成功
  输入：request={"signature": "签".repeat(20)}
  期望输出：success

[边界] 更新个人资料-签名最大4行-预期成功
  输入：request={"signature": "第1行\n第2行\n第3行\n第4行"}
  期望输出：success

[异类-异常] 更新个人资料-签名超长-预期拒绝
  输入：request={"signature": "签".repeat(21)}
  期望输出：ErrorCode.BAD_REQUEST, "个人签名最多 20 字"

[异类-异常] 更新个人资料-签名超4行-预期拒绝
  输入：request={"signature": "1\n2\n3\n4\n5"}
  期望输出：ErrorCode.BAD_REQUEST, "个人签名最多 4 行"

[异类-越权] 更新个人资料-普通用户修改邮箱-预期拒绝
  输入：request={"email": "new@test.com"}
  期望输出：ErrorCode.FORBIDDEN, "真实姓名、邮箱、工号、手机号及密码由管理员统一维护"

[异类-越权] 更新个人资料-普通用户修改realName-预期拒绝
  输入：request={"realName": "新名字"}
  期望输出：ErrorCode.FORBIDDEN, "用户名和真实姓名由组织统一维护"

[异类-异常] 更新个人资料-邮箱格式错误-预期拒绝
  输入：request={"email": "invalid-email"}
  期望输出：ErrorCode.INVALID_EMAIL_FORMAT

[异类-异常] 更新个人资料-手机号格式错误-预期拒绝
  输入：request={"phone": "12345"}
  期望输出：ErrorCode.INVALID_PHONE_FORMAT


【函数签名 - changePassword】
@PutMapping("/me/password")
public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody Map<String, String> request)

【输入参数】
request = {
    "oldPassword": "string, 旧密码",
    "newPassword": "string, 新密码，至少6位，UTF-8不超过72字节"
}

【权限要求】仅 ADMIN 可调用

--------------------------------------------------------------------------------
测试用例骨架 - 密码修改
--------------------------------------------------------------------------------

[同类] 密码修改-正常修改-预期成功
  前置：当前用户角色="ADMIN"
  输入：request={"oldPassword": "oldPass123", "newPassword": "newPass456"}
  期望输出：success
  数据库验证：password_hash已更新

[异类-越权] 密码修改-非ADMIN操作-预期拒绝
  前置：当前用户角色="EMPLOYEE"
  输入：request={"oldPassword": "oldPass123", "newPassword": "newPass456"}
  期望输出：ErrorCode.FORBIDDEN, "密码由管理员统一维护"

[异类-异常] 密码修改-旧密码错误-预期拒绝
  输入：request={"oldPassword": "wrongOld", "newPassword": "newPass456"}
  期望输出：ErrorCode.INVALID_CREDENTIALS

[异类-异常] 密码修改-新密码与旧密码相同-预期拒绝
  输入：request={"oldPassword": "samePass", "newPassword": "samePass"}
  期望输出：ErrorCode.BAD_REQUEST, "新密码不能与原密码相同"

[异类-异常] 密码修改-新密码过短-预期拒绝
  输入：request={"oldPassword": "oldPass123", "newPassword": "123"}
  期望输出：ErrorCode.PASSWORD_TOO_SHORT

[异类-异常] 密码修改-新密码超长UTF-8-预期拒绝
  输入：request={"oldPassword": "oldPass123", "newPassword": "a".repeat(73)}
  期望输出：ErrorCode.BAD_REQUEST, "密码不能超过72个UTF-8字节"


【函数签名 - uploadAvatar】
@PostMapping("/me/avatar")
public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "source", required = false) String source)

【输入参数】
file: 头像图片文件，支持JPEG/PNG/WEBP，最大5MB
source: 来源（可选）

【输出结构】
成功 = {"avatarUrl": url}
异常 = BusinessException(ErrorCode, message)

--------------------------------------------------------------------------------
测试用例骨架 - 头像上传
--------------------------------------------------------------------------------

[同类] 头像上传-JPEG格式-预期成功
  输入：file=test.jpg (JPEG, 1MB)
  期望输出：{"avatarUrl": "http://..."}
  数据库验证：avatar字段已更新

[同类] 头像上传-PNG格式-预期成功
  输入：file=test.png (PNG, 1MB)
  期望输出：成功

[同类] 头像上传-WEBP格式-预期成功
  输入：file=test.webp (WEBP, 1MB)
  期望输出：成功

[边界] 头像上传-最大5MB-预期成功
  输入：file=5MB.jpg (JPEG, 5MB)
  期望输出：成功

[异类-异常] 头像上传-空文件-预期拒绝
  输入：file=空文件
  期望输出：ErrorCode.MISSING_REQUIRED_FIELD, "请选择图片文件"

[异类-异常] 头像上传-文件过大-预期拒绝
  输入：file=6MB.jpg (JPEG, 6MB)
  期望输出：ErrorCode.FILE_TOO_LARGE

[异类-异常] 头像上传-格式不支持-预期拒绝
  输入：file=test.gif (GIF)
  期望输出：ErrorCode.UNSUPPORTED_IMAGE_FORMAT


================================================================================
八、用户查询与权限模块（UserController/PermissionController）- 中优先级
================================================================================

【函数签名 - getUserPanel】
@GetMapping("/{userId}/panel")
public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPanel(@PathVariable Long userId)

【输出结构】
{
    "user": {用户信息},
    "tasks": [任务列表],
    "logs": [日志列表],
    "top10Works": [Top10工作列表]
}

--------------------------------------------------------------------------------
测试用例骨架 - 用户面板查询
--------------------------------------------------------------------------------

[同类] 用户面板查询-正常查询-预期成功
  输入：userId=1
  期望输出：包含user/tasks/logs/top10Works

[异类-异常] 用户面板查询-用户不存在-预期拒绝
  输入：userId=99999
  期望输出：ErrorCode.USER_NOT_FOUND

[异类-越权] 用户面板查询-普通用户查看ADMIN面板-预期拒绝
  前置：目标用户role="ADMIN"
  输入：userId=1, 当前用户role="EMPLOYEE"
  期望输出：ErrorCode.USER_NOT_FOUND（隐藏ADMIN信息）


【函数签名 - getOrgOptions】
@GetMapping("/org-options")
public ResponseEntity<ApiResponse<Map<String, Object>>> getOrgOptions()

【输出结构】
{"departments": [...], "teams": [...]}

【权限要求】仅 ADMIN 可访问

[同类] 获取组织选项-ADMIN获取-预期成功
  前置：当前用户role="ADMIN"
  输入：无
  期望输出：包含departments和teams

[异类-越权] 获取组织选项-非ADMIN-预期拒绝
  前置：当前用户role="EMPLOYEE"
  输入：无
  期望输出：HttpStatus.FORBIDDEN, "无权操作"


【函数签名 - getSettings / updateSettings】
@GetMapping("/settings") / @PutMapping("/settings")

[同类] 获取用户设置-正常获取-预期成功
  输入：无
  期望输出：{"showCompletedTasks": true/false, "showYesterdayLog": true/false, ...}

[同类] 更新用户设置-更新显示设置-预期成功
  输入：{"showCompletedTasks": false, "showYesterdayLog": true}
  期望输出：success
  数据库验证：对应字段已更新


【函数签名 - getUserRelations】
@GetMapping("/relations")
public ResponseEntity<ApiResponse<Map<String, Object>>> getUserRelations()

【输出结构】
{"superiors": [...], "subordinates": [...]}

[同类] 获取用户关系-正常获取-预期成功
  输入：无
  期望输出：包含superiors和subordinates列表


================================================================================
九、权限检查模块（RoleChecker/PermissionController）- 高优先级
================================================================================

【函数签名 - RoleChecker.isAdminOnly】
public static boolean isAdminOnly(String role)

【输出结构】
boolean: true仅当role="ADMIN"

--------------------------------------------------------------------------------
测试用例骨架 - 角色检查
--------------------------------------------------------------------------------

[同类] 角色检查-ADMIN角色-预期true
  输入：role="ADMIN"
  期望输出：true

[同类] 角色检查-FOUNDER角色-预期false
  输入：role="FOUNDER"
  期望输出：false

[同类] 角色检查-DEPT_HEAD角色-预期false
  输入：role="DEPT_HEAD"
  期望输出：false

[边界] 角色检查-null角色-预期false
  输入：role=null
  期望输出：false

[边界] 角色检查-空字符串-预期false
  输入：role=""
  期望输出：false


【函数签名 - OrganizationRules.isBusinessRole】
public static boolean isBusinessRole(String role)

【输出结构】
boolean: true当role为FOUNDER/DEPT_HEAD/TEAM_LEADER/EMPLOYEE

[同类] 业务角色检查-FOUNDER-预期true
  输入：role="FOUNDER"
  期望输出：true

[同类] 业务角色检查-ADMIN-预期false
  输入：role="ADMIN"
  期望输出：false

[异类-异常] 业务角色检查-无效角色-预期false
  输入：role="INVALID"
  期望输出：false


【函数签名 - OrganizationRules.hasDepartment / hasTeam】
public static boolean hasDepartment(String role)
public static boolean hasTeam(String role)

[同类] 部门需求检查-DEPT_HEAD需要部门-预期true
  输入：role="DEPT_HEAD"
  期望输出：hasDepartment=true

[同类] 团队需求检查-EMPLOYEE需要团队-预期true
  输入：role="EMPLOYEE"
  期望输出：hasTeam=true

[同类] 部门需求检查-FOUNDER不需要部门-预期false
  输入：role="FOUNDER"
  期望输出：hasDepartment=false


================================================================================
十、AI生成测试代码指引
================================================================================

【生成要求】
1. 使用JUnit 5 + Spring Boot Test
2. 使用@MockBean模拟JdbcTemplate等依赖
3. 使用@Sql预置测试数据
4. 验证HTTP状态码和响应体
5. 验证数据库状态（使用@Sql或assertj-db）
6. 每个测试方法命名：场景标注_输入特征_预期动作

【示例测试方法命名】
- createAccount_正常员工创建_预期成功
- createAccount_工号重复_预期拒绝
- updateUserOrg_非ADMIN操作_预期403
- employment_员工入职分配部门_预期成功

================================================================================

骨架结构已生成。请确认：
1. 是否需要补充更多函数的测试用例？
2. 是否需要调整输入/输出格式？
3. 是否需要添加更多边界值测试？

确认后我将生成完整的测试用例文档。

================================================================================