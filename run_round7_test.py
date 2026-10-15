#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Project Pandora Round7 全功能回归测试脚本"""
import json, time, os, sys, urllib.request, urllib.error

BASE_URL = "http://localhost:8080/api"
REPORT_DIR = "test-results/round7"
os.makedirs(REPORT_DIR, exist_ok=True)

results = []

def record(tc_id, desc, passed, actual="", expected="", error=""):
    results.append({"id": tc_id, "desc": desc, "passed": passed,
                     "actual": actual, "expected": expected, "error": error})
    status = "PASS" if passed else "FAIL"
    print(f"  [{status}] {tc_id}: {desc}")
    if error: print(f"         Error: {error[:200]}")

def api(method, path, token=None, data=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token: headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try: return json.loads(e.read().decode("utf-8"))
        except: return {"code": e.code, "message": str(e)}
    except Exception as e:
        return {"code": -1, "message": str(e)}

def login(username, password):
    resp = api("POST", "/auth/login", data={"username": username, "password": password})
    if resp.get("code") == 200 and resp.get("data"):
        return resp["data"].get("token"), resp["data"]
    return None, resp

ACCOUNTS = [
    ("admin", "admin123", "ADMIN", 1),
    ("founder", "admin123", "FOUNDER", 2),
    ("dept_head", "admin123", "DEPT_HEAD", 3),
    ("team_leader", "admin123", "TEAM_LEADER", 4),
    ("employee", "admin123", "EMPLOYEE", 5),
    ("zhang_san", "admin123", "EMPLOYEE", 6),
    ("li_si", "admin123", "TEAM_LEADER", 7),
    ("yjl_founder", "123465", "FOUNDER", 100),
    ("yjl_dept_head", "123465", "DEPT_HEAD", 101),
    ("yjl_team_leader", "123465", "TEAM_LEADER", 102),
    ("yjl_employee", "123465", "EMPLOYEE", 103),
    ("yjl_employee2", "123465", "EMPLOYEE", 104),
]

print("=" * 60)
print("Project Pandora Round7 全功能回归测试")
print("=" * 60)

# ===== 模块1: 认证与注册测试 =====
print("\n--- 模块1: 认证与注册测试 ---")
tokens = {}
for i, (username, password, role, uid) in enumerate(ACCOUNTS):
    token, data = login(username, password)
    passed = token is not None
    record(f"TC-AUTH-{i+1:03d}",
           f"登录 {username} ({role})",
           passed, f"token={'yes' if token else 'no'}", "token=yes",
           "" if passed else str(data))
    if token: tokens[username] = token

# 账号信息一致性
print("\n  --- 账号信息一致性 ---")
for username, password, role, uid in ACCOUNTS:
    token = tokens.get(username)
    if not token: continue
    resp = api("GET", "/user/me", token=token)
    if resp.get("code") == 200 and resp.get("data"):
        d = resp["data"]
        actual_id = d.get("id", d.get("ID"))
        actual_role = d.get("role", d.get("ROLE"))
        actual_email = d.get("email", d.get("EMAIL"))
        passed = str(actual_id) == str(uid) and actual_role == role
        record(f"TC-AUTH-INFO-{uid}", f"用户信息一致性 {username}",
               passed, f"id={actual_id},role={actual_role},email={actual_email}",
               f"id={uid},role={role}", "" if passed else "信息不匹配")

# 错误凭据
resp = api("POST", "/auth/login", data={"username": "admin", "password": "wrongpass"})
record("TC-AUTH-ERR-001", "错误密码被拒绝", resp.get("code") != 200, f"code={resp.get('code')}", "code!=200")
resp = api("POST", "/auth/login", data={"username": "nonexistent", "password": "123465"})
record("TC-AUTH-ERR-002", "不存在用户被拒绝", resp.get("code") != 200, f"code={resp.get('code')}", "code!=200")

# ===== 模块2: 任务管理测试 =====
print("\n--- 模块2: 任务管理测试 ---")
admin_token = tokens.get("admin")
founder_token = tokens.get("yjl_founder")
employee_token = tokens.get("yjl_employee")
team_leader_token = tokens.get("yjl_team_leader")

# 任务创建
resp = api("POST", "/task", token=founder_token,
           data={"name":"Round7测试任务-创始人派发","priority":"MEDIUM","assigneeId":101,"isImportant":True,"isUrgent":False})
task_id1 = resp.get("data",{}).get("id") if resp.get("code")==200 else None
record("TC-TASK-001", "创始人向部门老总派发任务", task_id1 is not None, f"taskId={task_id1}", "taskId!=null")

resp = api("POST", "/task", token=team_leader_token,
           data={"name":"Round7测试任务-团队长派发","priority":"HIGH","assigneeId":103,"isImportant":False,"isUrgent":True})
task_id2 = resp.get("data",{}).get("id") if resp.get("code")==200 else None
record("TC-TASK-002", "团队长向员工派发任务", task_id2 is not None, f"taskId={task_id2}", "taskId!=null")

# 越权派发
resp = api("POST", "/task", token=employee_token,
           data={"name":"越权任务","priority":"MEDIUM","assigneeId":104,"isImportant":False,"isUrgent":False})
record("TC-TASK-003", "员工越权派发被拒绝", resp.get("code")==4006, f"code={resp.get('code')}", "code=4006")

# 任务进度更新
if task_id2:
    resp = api("PATCH", f"/task/{task_id2}/progress", token=employee_token,
               data={"progressNote":"Round7测试进度更新","status":"IN_PROGRESS"})
    record("TC-TASK-004", "员工更新任务进度", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 四象限查询
resp = api("GET", "/task/quadrant", token=employee_token)
quadrant_ok = resp.get("code")==200 and resp.get("data") and all(k in resp["data"] for k in ["importantUrgent","importantNotUrgent","notImportantUrgent","notImportantNotUrgent"])
record("TC-TASK-005", "四象限查询", quadrant_ok, f"code={resp.get('code')}", "code=200+4列表")

# 任务删除
if task_id1:
    resp = api("DELETE", f"/task/{task_id1}", token=founder_token)
    record("TC-TASK-006", "创始人删除自己派发的任务", resp.get("code")==200, f"code={resp.get('code')}", "code=200")
    resp2 = api("GET", f"/task/{task_id1}", token=founder_token)
    record("TC-TASK-007", "已删除任务不可查询", resp2.get("code")==404, f"code={resp2.get('code')}", "code=404")

# ===== 模块3: 工作日志测试 =====
print("\n--- 模块3: 工作日志测试 ---")
log_id = None
for _day in range(15, 31):
    log_date = f"2026-11-{_day:02d}"
    resp = api("POST", "/log", token=employee_token,
               data={"workItem":"Round7测试日志：完成全功能回归测试脚本编写和执行","logDate":log_date})
    if resp.get("code")==200:
        log_id = resp.get("data",{}).get("id")
        break
record("TC-LOG-001", f"创建工作日志({log_date})", log_id is not None, f"logId={log_id},date={log_date}", "logId!=null")

# 日志自动分析
if log_id:
    print(f"  等待日志自动分析(logId={log_id})...")
    analysis_found = False
    for i in range(15):
        time.sleep(2)
        resp = api("GET", "/ai/auto-analysis/latest", token=employee_token)
        if resp.get("code")==200 and resp.get("data"):
            d = resp["data"]
            if d.get("triggerLogId")==log_id or d.get("trigger_log_id")==log_id:
                analysis_found = True
                source = d.get("source","")
                record("TC-LOG-002", "日志自动分析触发", True,
                       f"source={source},keywords={d.get('keywords','')[:60]}", "source=LOCAL/EXTERNAL")
                break
    if not analysis_found:
        record("TC-LOG-002", "日志自动分析触发", False, "未找到分析记录", "30秒内应有分析记录")

# 日志草稿
resp = api("POST", "/log/draft", token=employee_token)
record("TC-LOG-003", "日志草稿生成", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 日志列表
resp = api("GET", "/log/list", token=employee_token)
record("TC-LOG-004", "日志列表查询", resp.get("code")==200 and isinstance(resp.get("data"),list),
       f"code={resp.get('code')},count={len(resp.get('data',[]))}", "code=200")

# ===== 模块4: AI分析测试 =====
print("\n--- 模块4: AI分析测试 ---")
# 关键词分析
resp = api("POST", "/ai/keyword", token=employee_token,
           data={"text":"完成项目开发编写测试文档部署上线修复bug"})
record("TC-AI-001", "关键词分析", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# AI建议
resp = api("GET", "/ai/suggestion", token=employee_token)
record("TC-AI-002", "AI建议生成", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# AI通道状态
resp = api("GET", "/ai/channel-status", token=admin_token)
record("TC-AI-003", "AI通道状态查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 自动分析历史
resp = api("GET", "/ai/auto-analysis/history", token=employee_token)
record("TC-AI-004", "自动分析历史查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# ===== 模块5: 视图与导图测试 =====
print("\n--- 模块5: 视图与导图测试 ---")
# 导图
resp = api("GET", "/view/mindmap", token=employee_token)
mindmap_ok = resp.get("code")==200 and resp.get("data") and all(k in resp["data"] for k in ["companyImportant","companyDispatch","personalImportant","personalLog"])
record("TC-VIEW-001", "导图四板块查询", mindmap_ok, f"code={resp.get('code')}", "code=200+4板块")

# 月视图
resp = api("GET", "/view/month?year=2026&month=10", token=employee_token)
record("TC-VIEW-002", "月视图查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 周视图
resp = api("GET", "/view/week?date=2026-10-10", token=employee_token)
record("TC-VIEW-003", "周视图查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 日视图
resp = api("GET", "/view/day?date=2026-10-10", token=employee_token)
record("TC-VIEW-004", "日视图查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 信息图谱
resp = api("GET", "/view/info-map", token=employee_token)
record("TC-VIEW-005", "信息图谱查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# ===== 模块6: 权限控制测试 =====
print("\n--- 模块6: 权限控制测试 ---")
# 功能权限码
resp = api("GET", "/permission/function/codes", token=admin_token)
record("TC-PERM-001", "功能权限码查询(admin)", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

resp = api("GET", "/permission/function/codes", token=founder_token)
record("TC-PERM-002", "功能权限码查询(founder)", resp.get("code")==200 or resp.get("code")==4006,
       f"code={resp.get('code')}", "code=200或4006")

# 权限主体列表
resp = api("GET", "/permission/function/subjects?functionCode=COMPANY_IMPORTANT_EDIT", token=admin_token)
record("TC-PERM-003", "权限主体列表查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# ===== 模块7: 通知与审计测试 =====
print("\n--- 模块7: 通知与审计测试 ---")
# 通知列表
resp = api("GET", "/notification/list", token=employee_token)
record("TC-NOTIF-001", "通知列表查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 未读通知
resp = api("GET", "/notification/list?unreadOnly=true", token=employee_token)
record("TC-NOTIF-002", "未读通知查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 审计日志
resp = api("GET", "/audit/my-today", token=admin_token)
record("TC-AUDIT-001", "今日审计日志查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

resp = api("GET", "/audit/list", token=admin_token)
record("TC-AUDIT-002", "审计日志列表查询(admin)", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

resp = api("GET", "/audit/list", token=employee_token)
record("TC-AUDIT-003", "审计日志列表查询(employee被拒)", resp.get("code")==403 or resp.get("code")==4006,
       f"code={resp.get('code')}", "code=403或4006")

# ===== 模块8: Top10管理测试 =====
print("\n--- 模块8: Top10管理测试 ---")
# 公司重要事项
resp = api("GET", "/top10/company-important", token=admin_token)
record("TC-TOP10-001", "公司重要事项查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 公司派发
resp = api("GET", "/top10/company-dispatch", token=admin_token)
record("TC-TOP10-002", "公司任务派发查询", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 个人重要事项
resp = api("GET", "/view/mindmap", token=employee_token)
pi_list = resp.get("data",{}).get("personalImportant",[]) if resp.get("code")==200 else []
record("TC-TOP10-003", "个人重要事项查询(导图)", resp.get("code")==200, f"count={len(pi_list)}", "code=200")

# 注入个人重要事项
resp = api("POST", "/view/personal-top10/inject", token=employee_token,
           data={"rankOrder":5,"content":"Round7测试注入项"})
record("TC-TOP10-004", "注入个人重要事项", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# ===== 模块9: 用户信息与密码重置测试 =====
print("\n--- 模块9: 用户信息与密码重置测试 ---")
# 用户列表
resp = api("GET", "/user/list", token=admin_token)
user_list_ok = resp.get("code")==200 and isinstance(resp.get("data"),list)
record("TC-USER-001", "用户列表查询", user_list_ok, f"code={resp.get('code')},count={len(resp.get('data',[]))}", "code=200")

# 按角色筛选
resp = api("GET", "/user/list?role=EMPLOYEE", token=admin_token)
employee_count = len(resp.get("data",[])) if resp.get("code")==200 else 0
record("TC-USER-002", "按角色筛选用户(EMPLOYEE)", resp.get("code")==200 and employee_count>0,
       f"count={employee_count}", "count>0")

# 下属用户
resp = api("GET", "/task/subordinate/users", token=team_leader_token)
record("TC-USER-003", "下属用户查询(团队长)", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# 下属任务
resp = api("GET", "/task/subordinate", token=team_leader_token)
record("TC-USER-004", "下属任务查询(团队长)", resp.get("code")==200, f"code={resp.get('code')}", "code=200")

# ===== 模块10: 基线数据保护验证 =====
print("\n--- 模块10: 基线数据保护验证 ---")
# 验证12个账号未被篡改
baseline_ok = True
for username, password, role, uid in ACCOUNTS:
    token = tokens.get(username)
    if not token: continue
    resp = api("GET", "/user/me", token=token)
    if resp.get("code")==200 and resp.get("data"):
        d = resp["data"]
        actual_role = d.get("role", d.get("ROLE"))
        if actual_role != role:
            baseline_ok = False
            record(f"TC-BASELINE-{uid}", f"基线用户{username}角色未篡改", False, f"role={actual_role}", f"role={role}")
record("TC-BASELINE-ALL", "所有基线用户信息未篡改", baseline_ok, "12账号全部验证", "角色匹配")

# 基线任务未被删除
resp = api("GET", "/task/list", token=admin_token)
if resp.get("code")==200:
    task_count = len(resp.get("data",[]))
    record("TC-BASELINE-TASK", "基线任务数据完整", task_count>0, f"totalTasks={task_count}", "tasks>0")

# ===== 生成测试报告 =====
print("\n" + "=" * 60)
print("生成测试报告...")
total = len(results)
passed = sum(1 for r in results if r["passed"])
failed = total - passed
pass_rate = f"{passed/total*100:.1f}%" if total>0 else "0%"

summary = {
    "total": total, "passed": passed, "failed": failed,
    "passRate": pass_rate, "timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
}

# JSON报告
report_json = {"summary": summary, "results": results}
json_path = os.path.join(REPORT_DIR, "test_report.json")
with open(json_path, "w", encoding="utf-8") as f:
    json.dump(report_json, f, ensure_ascii=False, indent=2)

# CSV报告
csv_path = os.path.join(REPORT_DIR, "test_report.csv")
with open(csv_path, "w", encoding="utf-8-sig") as f:
    f.write("用例编号,描述,结果,实际值,预期值,错误信息\n")
    for r in results:
        f.write(f"{r['id']},{r['desc']},{'PASS' if r['passed'] else 'FAIL'},{r['actual']},{r['expected']},{r['error']}\n")

# 摘要文件
summary_path = os.path.join(REPORT_DIR, "test_summary.txt")
with open(summary_path, "w", encoding="utf-8") as f:
    f.write("Project Pandora Round7 全功能回归测试报告\n")
    f.write("=" * 60 + "\n")
    f.write(f"测试时间: {summary['timestamp']}\n")
    f.write(f"总用例数: {total}\n")
    f.write(f"通过数:   {passed}\n")
    f.write(f"失败数:   {failed}\n")
    f.write(f"通过率:   {pass_rate}\n")
    f.write("=" * 60 + "\n\n")
    for r in results:
        status = "✅ PASS" if r["passed"] else "❌ FAIL"
        f.write(f"{status} {r['id']}: {r['desc']}\n")
        if r["error"]: f.write(f"       Error: {r['error']}\n")

print(f"\n测试完成!")
print(f"  总用例: {total} | 通过: {passed} | 失败: {failed} | 通过率: {pass_rate}")
print(f"  JSON报告: {json_path}")
print(f"  CSV报告:  {csv_path}")
print(f"  摘要文件: {summary_path}")
print("=" * 60)