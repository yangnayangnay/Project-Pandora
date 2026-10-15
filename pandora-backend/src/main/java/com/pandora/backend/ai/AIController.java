package com.pandora.backend.ai;

import com.pandora.backend.common.ApiResponse;
import com.pandora.backend.common.BusinessException;
import com.pandora.backend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.pandora.backend.audit.Auditable;

import java.util.*;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AIGatewayService aiGatewayService;

    @Value("${deepseek.api-url:}")
    private String deepseekApiUrl;

    @PostMapping("/mbti/submit")
    @Auditable(operationType = "AI_MBTI_SUBMIT", targetType = "AI_ANALYSIS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitMbti(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        List<Integer> answers = (List<Integer>) request.get("answers");
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "答题数据不能为空");
        }

        String personalityType = calculateMbtiType(answers);
        String reportContent;
        String aiSource;

        try {
            Map<String, Object> aiResult = aiGatewayService.invoke(
                    "请为MBTI性格类型 " + personalityType + " 生成一份详细的性格分析报告，包括性格特点、优势劣势和适合的职业方向。",
                    "MBTI_REPORT");
            if ("EXTERNAL".equals(aiResult.get("source"))) {
                reportContent = (String) aiResult.get("result");
                aiSource = "EXTERNAL";
            } else {
                reportContent = generateMbtiReportLocal(personalityType);
                aiSource = "LOCAL";
            }
        } catch (Exception e) {
            reportContent = generateMbtiReportLocal(personalityType);
            aiSource = "LOCAL";
        }

        jdbcTemplate.update(
                "INSERT INTO mbti_reports (user_id, personality_type, answer_data, report_content) VALUES (?, ?, ?, ?)",
                userId, personalityType, answers.toString(), reportContent
        );

        Long reportId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("reportId", reportId);
        result.put("personalityType", personalityType);
        result.put("reportContent", reportContent);
        result.put("source", aiSource);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/mbti/report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMbtiReport() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> report = jdbcTemplate.queryForMap(
                    "SELECT * FROM mbti_reports WHERE user_id = ? AND deleted = 0 ORDER BY generated_at DESC LIMIT 1", userId);
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无MBTI报告");
        }
    }

    @PostMapping("/mbti/report/generate")
    @Auditable(operationType = "AI_MBTI_REPORT_GENERATE", targetType = "AI_ANALYSIS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateMbtiReport(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        String personalityType = request.get("personalityType") != null ? request.get("personalityType").toString() : null;
        if (personalityType == null || !personalityType.matches("^[EI][SN][TF][JP]$")) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "性格类型不合法，须为^[EI][SN][TF][JP]$格式");
        }

        Object answersObj = request.get("answers");
        String answersStr = answersObj != null ? answersObj.toString() : null;

        String reportContent;
        String aiSource;

        try {
            Map<String, Object> aiResult = aiGatewayService.invoke(
                    "请为MBTI性格类型 " + personalityType + " 生成一份详细的性格分析报告，包括性格特点、优势劣势和适合的职业方向。",
                    "MBTI_REPORT");
            if ("EXTERNAL".equals(aiResult.get("source"))) {
                reportContent = (String) aiResult.get("result");
                aiSource = "EXTERNAL";
            } else {
                reportContent = generateMbtiReportLocal(personalityType);
                aiSource = "LOCAL";
            }
        } catch (Exception e) {
            reportContent = generateMbtiReportLocal(personalityType);
            aiSource = "LOCAL";
        }

        jdbcTemplate.update(
                "INSERT INTO mbti_reports (user_id, personality_type, answer_data, report_content) VALUES (?, ?, ?, ?)",
                userId, personalityType, answersStr, reportContent
        );

        Long reportId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("reportId", reportId);
        result.put("personalityType", personalityType);
        result.put("reportContent", reportContent);
        result.put("source", aiSource);

        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @PostMapping("/keyword")
    @Auditable(operationType = "AI_KEYWORD_ANALYSIS", targetType = "AI_ANALYSIS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> keywordAnalysis(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "分析文本不能为空");
        }

        List<Map<String, Object>> keywords;
        String aiSource;

        try {
            Map<String, Object> aiResult = aiGatewayService.invoke(
                    "分析以下文本的10个关键词及其出现次数，以JSON数组格式返回：\n" + text,
                    "KEYWORD_ANALYSIS");
            if ("EXTERNAL".equals(aiResult.get("source"))) {
                keywords = parseExternalKeywords((String) aiResult.get("result"));
                aiSource = "EXTERNAL";
            } else {
                keywords = extractKeywords(text);
                aiSource = "LOCAL";
            }
        } catch (Exception e) {
            keywords = extractKeywords(text);
            aiSource = "LOCAL";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("keywords", keywords);
        response.put("source", aiSource);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/suggestion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuggestion() {
        Long userId = getCurrentUserId();

        Map<String, Object> suggestion = new HashMap<>();

        Map<String, Object> userInfo = new HashMap<>();
        String role = "";
        String username = "";
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT username, role, real_name, department_id, team_id FROM users WHERE id = ? AND deleted = 0", userId);
            username = (String) user.get("USERNAME");
            role = (String) user.get("ROLE");
            userInfo.put("username", username);
            userInfo.put("role", role);
            userInfo.put("realName", user.get("REAL_NAME"));
            userInfo.put("departmentId", user.get("DEPARTMENT_ID"));
            userInfo.put("teamId", user.get("TEAM_ID"));
        } catch (Exception e) {
            userInfo.put("username", "未知");
            userInfo.put("role", "未知");
        }
        suggestion.put("userInfo", userInfo);

        Map<String, Object> taskStats = new HashMap<>();
        int totalTasks = 0, completedTasks = 0, inProgressTasks = 0, pendingTasks = 0, overdueTasks = 0;
        try {
            totalTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND deleted = 0", Integer.class, userId);
            completedTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND deleted = 0 AND status IN ('COMPLETED','CONFIRMED')", Integer.class, userId);
            inProgressTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND deleted = 0 AND status = 'IN_PROGRESS'", Integer.class, userId);
            pendingTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND deleted = 0 AND status = 'PENDING'", Integer.class, userId);
            overdueTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE assignee_id = ? AND deleted = 0 AND end_time IS NOT NULL AND end_time < CURRENT_TIMESTAMP AND status NOT IN ('COMPLETED','CONFIRMED')", Integer.class, userId);
        } catch (Exception ignored) {}
        double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;
        taskStats.put("totalTasks", totalTasks);
        taskStats.put("completedTasks", completedTasks);
        taskStats.put("inProgressTasks", inProgressTasks);
        taskStats.put("pendingTasks", pendingTasks);
        taskStats.put("overdueTasks", overdueTasks);
        taskStats.put("completionRate", Math.round(completionRate * 100) / 100.0);
        suggestion.put("taskStats", taskStats);

        Map<String, Object> logStats = new HashMap<>();
        int logCount = 0;
        try {
            logCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM work_logs WHERE user_id = ? AND deleted = 0", Integer.class, userId);
        } catch (Exception ignored) {}
        logStats.put("logCount", logCount);
        suggestion.put("logStats", logStats);

        String roleDisplay = getRoleDisplay(role);
        String workAdvice = generateWorkAdvice(role, totalTasks, completedTasks, inProgressTasks, pendingTasks, overdueTasks, logCount);
        String careerAdvice = generateCareerAdvice(role, completionRate, totalTasks, logCount);
        String developmentPlan = generateDevelopmentPlan(role, completionRate, totalTasks, overdueTasks);
        String aiSource = "LOCAL";

        try {
            final String fRoleDisplay = roleDisplay;
            final int fTotalTasks = totalTasks, fCompletedTasks = completedTasks, fInProgressTasks = inProgressTasks, fOverdueTasks = overdueTasks, fLogCount = logCount;
            java.util.concurrent.CompletableFuture<Map<String, Object>> aiFuture =
                    java.util.concurrent.CompletableFuture.supplyAsync(() ->
                            aiGatewayService.invoke(
                                    "用户角色：" + fRoleDisplay + "，任务总数：" + fTotalTasks + "，已完成：" + fCompletedTasks +
                                    "，进行中：" + fInProgressTasks + "，逾期：" + fOverdueTasks + "，日志数：" + fLogCount +
                                    "。请生成工作建议、职业发展建议和发展计划。",
                                    "SUGGESTION"));
            Map<String, Object> aiResult = aiFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
            if ("EXTERNAL".equals(aiResult.get("source"))) {
                workAdvice = (String) aiResult.get("result");
                aiSource = "EXTERNAL";
            }
        } catch (Exception ignored) {}

        suggestion.put("workAdvice", workAdvice);
        suggestion.put("careerAdvice", careerAdvice);
        suggestion.put("developmentPlan", developmentPlan);
        suggestion.put("source", aiSource);

        return ResponseEntity.ok(ApiResponse.success(suggestion));
    }

    private String getRoleDisplay(String role) {
        switch (role) {
            case "ADMIN": return "管理员";
            case "FOUNDER": return "创始人";
            case "DEPT_HEAD": return "部门老总";
            case "TEAM_LEADER": return "团队长";
            case "EMPLOYEE": return "员工";
            default: return role;
        }
    }

    private String generateWorkAdvice(String role, int total, int completed, int inProgress, int pending, int overdue, int logs) {
        StringBuilder sb = new StringBuilder();
        switch (role) {
            case "EMPLOYEE":
                sb.append("您当前共有 ").append(total).append(" 个任务");
                if (pending > 0) sb.append("，其中 ").append(pending).append(" 个待处理，建议尽快开始执行");
                if (inProgress > 0) sb.append("，").append(inProgress).append(" 个进行中，请保持进度");
                if (overdue > 0) sb.append("。注意：有 ").append(overdue).append(" 个任务已逾期，请优先处理");
                if (logs == 0) sb.append("。建议开始记录工作日志，有助于总结经验");
                else sb.append("。已记录 ").append(logs).append(" 条工作日志，保持良好习惯");
                break;
            case "TEAM_LEADER":
                sb.append("作为团队长，您负责 ").append(total).append(" 个任务");
                if (completionRateOf(completed, total) < 50) sb.append("，团队完成率偏低，建议加强任务跟进和进度督导");
                else sb.append("，团队完成率良好，继续保持");
                if (overdue > 0) sb.append("。有 ").append(overdue).append(" 个逾期任务，建议协调资源优先解决");
                sb.append("。建议定期查看下属工作日志，及时给予指导");
                break;
            case "DEPT_HEAD":
                sb.append("作为部门老总，关注部门整体进展。当前任务完成率 ");
                sb.append(completionRateOf(completed, total)).append("%");
                if (overdue > 0) sb.append("，有 ").append(overdue).append(" 个逾期任务需关注");
                sb.append("。建议关注团队长的工作分配合理性，优化资源配置");
                break;
            case "FOUNDER":
                sb.append("作为创始人，统筹全局。当前共 ").append(total).append(" 个任务，完成 ");
                sb.append(completed).append(" 个（").append(completionRateOf(completed, total)).append("%）");
                sb.append("。建议关注各部门关键指标，把控战略方向");
                break;
            case "ADMIN":
                sb.append("作为管理员，负责系统运维。当前系统共有 ").append(total).append(" 个任务");
                sb.append("。建议关注待审批账号和系统运行状态");
                break;
            default:
                sb.append("建议保持每日日志记录习惯，有助于提升工作效率");
        }
        return sb.toString();
    }

    private String generateCareerAdvice(String role, double completionRate, int totalTasks, int logs) {
        StringBuilder sb = new StringBuilder();
        double roundedRate = Math.round(completionRate * 100) / 100.0;
        switch (role) {
            case "EMPLOYEE":
                if (completionRate >= 80) sb.append("任务完成率优秀（").append(roundedRate).append("%），表现突出，可考虑承担更有挑战性的任务");
                else if (completionRate >= 50) sb.append("任务完成率良好（").append(roundedRate).append("%），建议提升时间管理能力，优先处理重要紧急任务");
                else sb.append("任务完成率待提升（").append(roundedRate).append("%），建议聚焦核心任务，减少分散精力");
                if (logs < 5) sb.append("。建议增加工作日志记录频率，积累经验便于复盘");
                break;
            case "TEAM_LEADER":
                sb.append("作为团队长，建议提升团队管理能力，关注成员成长");
                if (completionRate < 60) sb.append("。团队完成率偏低，建议优化任务分配和进度管理");
                sb.append("。可学习敏捷管理方法，提高团队协作效率");
                break;
            case "DEPT_HEAD":
                sb.append("作为部门老总，建议加强战略规划和资源统筹能力");
                sb.append("。关注部门间协作，推动跨部门项目顺利落地");
                break;
            case "FOUNDER":
                sb.append("作为创始人，建议聚焦公司战略方向和核心业务");
                sb.append("。关注团队建设和人才培养，为长期发展奠定基础");
                break;
            case "ADMIN":
                sb.append("作为管理员，建议熟悉系统各模块功能，提升运维效率");
                sb.append("。关注用户反馈，持续优化系统体验");
                break;
            default:
                sb.append("根据您的工作模式，建议加强团队协作和沟通能力");
        }
        return sb.toString();
    }

    private String generateDevelopmentPlan(String role, double completionRate, int totalTasks, int overdue) {
        StringBuilder sb = new StringBuilder();
        switch (role) {
            case "EMPLOYEE":
                sb.append("短期目标：提升任务完成率至 80% 以上");
                sb.append("；中期目标：掌握核心业务技能，承担更多重要任务");
                sb.append("；长期目标：向团队长或专业方向 发展");
                break;
            case "TEAM_LEADER":
                sb.append("短期目标：提升团队整体完成率，减少逾期任务");
                sb.append("；中期目标：优化团队工作流程，建立高效协作机制");
                sb.append("；长期目标：向部门管理方向发展，提升领导力");
                break;
            case "DEPT_HEAD":
                sb.append("短期目标：确保部门关键指标达标");
                sb.append("；中期目标：优化部门资源配置，提升整体效率");
                sb.append("；长期目标：参与公司战略决策，推动业务创新");
                break;
            case "FOUNDER":
                sb.append("短期目标：把控核心项目进度，确保关键节点交付");
                sb.append("；中期目标：完善组织架构，培养核心管理团队");
                sb.append("；长期目标：推动公司业务增长，实现战略目标");
                break;
            case "ADMIN":
                sb.append("短期目标：确保系统稳定运行，及时处理审批");
                sb.append("；中期目标：优化系统配置，提升用户体验");
                sb.append("；长期目标：完善系统功能，支撑业务发展");
                break;
            default:
                sb.append("短期目标：提升任务完成率；中期目标：承担更多核心任务；长期目标：向管理方向发展");
        }
        if (overdue > 0) {
            sb.append("。当前有 ").append(overdue).append(" 个逾期任务，建议优先处理");
        }
        return sb.toString();
    }

    private int completionRateOf(int completed, int total) {
        return total > 0 ? (int)(completed * 100.0 / total) : 0;
    }


    @GetMapping("/project-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectAnalysis() {
        Map<String, Object> result = new HashMap<>();

        result.put("overview", getProjectOverview());
        result.put("statusDistribution", getStatusDistribution());
        result.put("priorityAnalysis", getPriorityAnalysis());
        result.put("completionTrend", getCompletionTrend());
        result.put("overdueAnalysis", getOverdueAnalysis());
        result.put("teamRanking", getTeamRanking());
        result.put("suggestions", generateProjectSuggestions(result));

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Map<String, Object> getProjectOverview() {
        Map<String, Object> overview = new HashMap<>();
        try {
            Integer totalTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE deleted = 0", Integer.class);
            Integer completedTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE deleted = 0 AND status IN ('COMPLETED', 'CONFIRMED')", Integer.class);
            Integer inProgressTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE deleted = 0 AND status = 'IN_PROGRESS'", Integer.class);
            Integer pendingTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE deleted = 0 AND status = 'PENDING'", Integer.class);
            Integer rejectedTasks = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE deleted = 0 AND status = 'REJECTED'", Integer.class);

            totalTasks = totalTasks == null ? 0 : totalTasks;
            completedTasks = completedTasks == null ? 0 : completedTasks;

            double completionRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;

            overview.put("totalTasks", totalTasks);
            overview.put("completedTasks", completedTasks);
            overview.put("inProgressTasks", inProgressTasks);
            overview.put("pendingTasks", pendingTasks);
            overview.put("rejectedTasks", rejectedTasks);
            overview.put("completionRate", Math.round(completionRate * 100) / 100.0);
        } catch (Exception e) {
            overview.put("error", "统计数据查询失败");
        }
        return overview;
    }

    private List<Map<String, Object>> getStatusDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT status, COUNT(*) as count FROM tasks WHERE deleted = 0 GROUP BY status");
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("status", row.get("status"));
                item.put("count", row.get("count"));
                item.put("statusName", getStatusDisplayName((String) row.get("status")));
                distribution.add(item);
            }
        } catch (Exception e) {
            Map<String, Object> item = new HashMap<>();
            item.put("error", "状态分布查询失败");
            distribution.add(item);
        }
        return distribution;
    }

    private List<Map<String, Object>> getPriorityAnalysis() {
        List<Map<String, Object>> analysis = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT priority, " +
                    "COUNT(*) as total, " +
                    "SUM(CASE WHEN status IN ('COMPLETED','CONFIRMED') THEN 1 ELSE 0 END) as completed, " +
                    "SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgress " +
                    "FROM tasks WHERE deleted = 0 GROUP BY priority");
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<>();
                String priority = (String) row.get("priority");
                item.put("priority", priority);
                item.put("priorityName", getPriorityDisplayName(priority));
                item.put("total", row.get("total"));
                item.put("completed", row.get("completed"));
                item.put("inProgress", row.get("inProgress"));
                int total = ((Number) row.get("total")).intValue();
                int completed = ((Number) row.get("completed")).intValue();
                double rate = total > 0 ? (completed * 100.0 / total) : 0.0;
                item.put("completionRate", Math.round(rate * 100) / 100.0);
                analysis.add(item);
            }
        } catch (Exception e) {
            Map<String, Object> item = new HashMap<>();
            item.put("error", "优先级分析查询失败");
            analysis.add(item);
        }
        return analysis;
    }

    private List<Map<String, Object>> getCompletionTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT DATE(updated_at) as date, COUNT(*) as count " +
                    "FROM tasks WHERE deleted = 0 AND status IN ('COMPLETED','CONFIRMED') " +
                    "AND updated_at >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY) " +
                    "GROUP BY DATE(updated_at) ORDER BY date");
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", row.get("date").toString());
                item.put("completedCount", row.get("count"));
                trend.add(item);
            }
        } catch (Exception e) {
            Map<String, Object> item = new HashMap<>();
            item.put("error", "完成趋势查询失败");
            trend.add(item);
        }
        return trend;
    }

    private Map<String, Object> getOverdueAnalysis() {
        Map<String, Object> overdue = new HashMap<>();
        try {
            Integer overdueCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tasks WHERE deleted = 0 " +
                    "AND end_time IS NOT NULL AND end_time < CURRENT_TIMESTAMP " +
                    "AND status NOT IN ('COMPLETED','CONFIRMED')", Integer.class);
            overdueCount = overdueCount == null ? 0 : overdueCount;

            List<Map<String, Object>> overdueTasks = jdbcTemplate.queryForList(
                    "SELECT id, name, priority, status, assignee_id, end_time " +
                    "FROM tasks WHERE deleted = 0 " +
                    "AND end_time IS NOT NULL AND end_time < CURRENT_TIMESTAMP " +
                    "AND status NOT IN ('COMPLETED','CONFIRMED') " +
                    "ORDER BY end_time ASC LIMIT 10");

            overdue.put("overdueCount", overdueCount);
            overdue.put("overdueTasks", overdueTasks);
        } catch (Exception e) {
            overdue.put("error", "逾期分析查询失败");
        }
        return overdue;
    }

    private List<Map<String, Object>> getTeamRanking() {
        List<Map<String, Object>> ranking = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT u.username, u.role, " +
                    "COUNT(t.id) as totalTasks, " +
                    "SUM(CASE WHEN t.status IN ('COMPLETED','CONFIRMED') THEN 1 ELSE 0 END) as completedTasks " +
                    "FROM users u LEFT JOIN tasks t ON u.id = t.assignee_id AND t.deleted = 0 " +
                    "WHERE u.deleted = 0 " +
                    "GROUP BY u.id, u.username, u.role " +
                    "ORDER BY completedTasks DESC");
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("username", row.get("username"));
                item.put("role", row.get("role"));
                item.put("totalTasks", row.get("totalTasks"));
                item.put("completedTasks", row.get("completedTasks"));
                int total = ((Number) row.get("totalTasks")).intValue();
                int completed = ((Number) row.get("completedTasks")).intValue();
                double rate = total > 0 ? (completed * 100.0 / total) : 0.0;
                item.put("completionRate", Math.round(rate * 100) / 100.0);
                ranking.add(item);
            }
        } catch (Exception e) {
            Map<String, Object> item = new HashMap<>();
            item.put("error", "团队排名查询失败");
            ranking.add(item);
        }
        return ranking;
    }

    private List<String> generateProjectSuggestions(Map<String, Object> analysisData) {
        List<String> suggestions = new ArrayList<>();

        try {
            Map<String, Object> overview = (Map<String, Object>) analysisData.get("overview");
            Number completionRateNum = (Number) overview.get("completionRate");
            double completionRate = completionRateNum != null ? completionRateNum.doubleValue() : 0.0;

            if (completionRate >= 80) {
                suggestions.add("项目整体完成率达到 " + completionRate + "%，表现优秀，建议保持当前节奏");
            } else if (completionRate >= 50) {
                suggestions.add("项目整体完成率 " + completionRate + "%，进展正常，建议关注未完成任务");
            } else if (completionRate > 0) {
                suggestions.add("项目整体完成率仅 " + completionRate + "%，需加快进度，建议优先处理重要紧急任务");
            } else {
                suggestions.add("暂无已完成任务，建议尽快推进任务执行");
            }

            Map<String, Object> overdueAnalysis = (Map<String, Object>) analysisData.get("overdueAnalysis");
            Number overdueCount = (Number) overdueAnalysis.get("overdueCount");
            int overdueCnt = overdueCount != null ? overdueCount.intValue() : 0;
            if (overdueCnt > 0) {
                suggestions.add("当前有 " + overdueCnt + " 个逾期任务，建议优先处理逾期任务并调整截止时间");
            } else {
                suggestions.add("暂无逾期任务，时间管理良好");
            }

            List<Map<String, Object>> priorityAnalysis = (List<Map<String, Object>>) analysisData.get("priorityAnalysis");
            for (Map<String, Object> pa : priorityAnalysis) {
                String priority = (String) pa.get("priority");
                Number rate = (Number) pa.get("completionRate");
                if ("URGENT".equals(priority) && rate != null && rate.doubleValue() < 50) {
                    suggestions.add("紧急任务完成率仅 " + rate + "%，建议集中资源优先处理");
                    break;
                }
            }

            List<Map<String, Object>> trend = (List<Map<String, Object>>) analysisData.get("completionTrend");
            if (trend.isEmpty()) {
                suggestions.add("近7天无任务完成记录，建议加强日常任务推进");
            } else {
                suggestions.add("近7天有 " + trend.size() + " 天有任务完成，保持良好工作节奏");
            }
        } catch (Exception e) {
            suggestions.add("建议生成失败，请检查数据完整性");
        }

        return suggestions;
    }

    private String getStatusDisplayName(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "PENDING": return "待接收";
            case "IN_PROGRESS": return "进行中";
            case "COMPLETED": return "已完成";
            case "CONFIRMED": return "已确认";
            case "REJECTED": return "已退回";
            default: return status;
        }
    }

    private String getPriorityDisplayName(String priority) {
        if (priority == null) return "中";
        switch (priority) {
            case "LOW": return "低";
            case "MEDIUM": return "中";
            case "HIGH": return "高";
            case "URGENT": return "紧急";
            default: return priority;
        }
    }

    private String calculateMbtiType(List<Integer> answers) {
        int e = 0, i = 0, s = 0, n = 0, t = 0, f = 0, j = 0, p = 0;

        for (int idx = 0; idx < answers.size(); idx++) {
            int ans = answers.get(idx);
            if (idx % 4 == 0) { if (ans == 1) e++; else i++; }
            else if (idx % 4 == 1) { if (ans == 1) s++; else n++; }
            else if (idx % 4 == 2) { if (ans == 1) t++; else f++; }
            else { if (ans == 1) j++; else p++; }
        }

        return (e > i ? "E" : "I") + (s > n ? "S" : "N") + (t > f ? "T" : "F") + (j > p ? "J" : "P");
    }

    private String generateMbtiReportLocal(String type) {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("INTJ", "建筑师：富有想象力但决策果断，雄心壮志但深谋远虑。适合战略规划、系统架构等岗位。");
        descriptions.put("INTP", "逻辑学家：具有创造力的思考者，对知识有强烈的渴望。适合研究、分析、开发等岗位。");
        descriptions.put("ENTJ", "指挥官：大胆、富有想象力且意志坚定的天生领导者。适合管理、创业等岗位。");
        descriptions.put("ENTP", "辩论家：聪明好奇的思想者，不会拒绝任何智力的挑战。适合创新、咨询等岗位。");
        descriptions.put("INFJ", "提倡者：安静而神秘，同时鼓舞人心的不知疲倦的理想主义者。适合咨询、教育等岗位。");
        descriptions.put("INFP", "调停者：诗意、善良的利他主义者，总是热心为正当事业提供帮助。适合创意、公益等岗位。");
        descriptions.put("ENFJ", "主人公：富有魅力鼓舞人心的领导者，能够使听众为之着迷。适合领导、培训等岗位。");
        descriptions.put("ENFP", "竞选者：热情、有同情心、有创造力的人，善于社交。适合营销、创意等岗位。");
        descriptions.put("ISTJ", "物流师：务实且注重事实的可靠者，责任心强。适合执行、管理等岗位。");
        descriptions.put("ISFJ", "守卫者：非常专注而温暖的守护者，时刻准备着保护爱着的人们。适合服务、支持等岗位。");
        descriptions.put("ESTJ", "总经理：务实且注重事实的可靠者，传统和秩序的维护者。适合管理、运营等岗位。");
        descriptions.put("ESFJ", "执政官：极有同情心，爱交际受欢迎的人们。适合人事、客户服务等岗位。");
        descriptions.put("ISTP", "鉴赏家：大胆而实际的实验家，善于使用各种工具。适合技术、工程等岗位。");
        descriptions.put("ISFP", "探险家：灵活有魅力的艺术家，时刻准备着探索和体验新事物。适合设计、艺术等岗位。");
        descriptions.put("ESTP", "企业家：聪明、精力充沛善于感知的人们，真心享受生活在边缘地带。适合创业、销售等岗位。");
        descriptions.put("ESFP", "表演者：自发的、精力充沛而热情的表演者。适合娱乐、公关等岗位。");

        String desc = descriptions.getOrDefault(type, "独特的性格类型，具有自身独特的优势和适合的发展方向。");
        return "MBTI性格类型: " + type + "\n\n" + desc;
    }

    private List<Map<String, Object>> extractKeywords(String text) {
        String[] words = text.split("[\\s,，。.、！!？?；;]+");
        Map<String, Integer> frequency = new HashMap<>();

        for (String word : words) {
            if (word.length() >= 2) {
                frequency.put(word, frequency.getOrDefault(word, 0) + 1);
            }
        }

        List<Map<String, Object>> keywords = new ArrayList<>();
        frequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(entry -> {
                    Map<String, Object> keyword = new HashMap<>();
                    keyword.put("word", entry.getKey());
                    keyword.put("count", entry.getValue());
                    keywords.add(keyword);
                });

        return keywords;
    }

    private List<Map<String, Object>> parseExternalKeywords(String aiResponse) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return keywords;
        }
        String[] lines = aiResponse.split("\n");
        for (String line : lines) {
            line = line.trim().replaceAll("^\\d+[.、)]\\s*", "");
            if (!line.isEmpty()) {
                Map<String, Object> keyword = new HashMap<>();
                keyword.put("word", line);
                keyword.put("count", 1);
                keywords.add(keyword);
            }
            if (keywords.size() >= 10) break;
        }
        return keywords;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/auto-analysis/latest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLatestAutoAnalysis() {
        Long userId = getCurrentUserId();
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT * FROM ai_analyses WHERE user_id = ? AND analysis_type = 'LOG_AUTO_ANALYSIS' AND deleted = 0 ORDER BY generated_at DESC LIMIT 1", userId);
            return ResponseEntity.ok(ApiResponse.success(formatAutoAnalysis(row)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无自动分析结果");
        }
    }

    @GetMapping("/auto-analysis/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAutoAnalysisHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = getCurrentUserId();
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM ai_analyses WHERE user_id = ? AND analysis_type = 'LOG_AUTO_ANALYSIS' AND deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (startDate != null) {
            sql.append(" AND generated_at >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND generated_at <= ?");
            params.add(endDate + " 23:59:59");
        }
        sql.append(" ORDER BY generated_at DESC");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(formatAutoAnalysis(row));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Map<String, Object> formatAutoAnalysis(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", row.get("id") != null ? row.get("id") : row.get("ID"));
        result.put("triggerLogId", row.get("trigger_log_id") != null ? row.get("trigger_log_id") : row.get("TRIGGER_LOG_ID"));
        result.put("analysisContent", row.get("result") != null ? row.get("result") : row.get("RESULT"));
        result.put("mbtiType", row.get("mbti_type") != null ? row.get("mbti_type") : row.get("MBTI_TYPE"));
        result.put("source", row.get("source") != null ? row.get("source") : row.get("SOURCE"));
        result.put("generatedAt", row.get("generated_at") != null ? row.get("generated_at") : row.get("GENERATED_AT"));
        Object keywordsRaw = row.get("keywords");
        if (keywordsRaw == null) keywordsRaw = row.get("KEYWORDS");
        result.put("keywords", keywordsRaw != null ? keywordsRaw.toString() : "[]");
        return result;
    }
}