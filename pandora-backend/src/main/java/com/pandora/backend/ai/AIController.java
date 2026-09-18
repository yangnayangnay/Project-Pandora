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

import java.util.*;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${deepseek.api-url:}")
    private String deepseekApiUrl;

    @PostMapping("/mbti/submit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitMbti(@RequestBody Map<String, Object> request) {
        Long userId = getCurrentUserId();
        List<Integer> answers = (List<Integer>) request.get("answers");
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "答题数据不能为空");
        }

        String personalityType = calculateMbtiType(answers);
        String reportContent = generateMbtiReport(personalityType);

        jdbcTemplate.update(
                "INSERT INTO mbti_reports (user_id, personality_type, answer_data, report_content) VALUES (?, ?, ?, ?)",
                userId, personalityType, answers.toString(), reportContent
        );

        Long reportId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("reportId", reportId);
        result.put("personalityType", personalityType);
        result.put("reportContent", reportContent);

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

    @PostMapping("/keyword")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> keywordAnalysis(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "分析文本不能为空");
        }

        List<Map<String, Object>> keywords = extractKeywords(text);
        return ResponseEntity.ok(ApiResponse.success(keywords));
    }

    @GetMapping("/suggestion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuggestion() {
        Long userId = getCurrentUserId();
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("workAdvice", "建议保持每日日志记录习惯，有助于提升工作效率");
        suggestion.put("careerAdvice", "根据您的工作模式，建议加强团队协作和沟通能力");
        suggestion.put("developmentPlan", "短期目标：提升任务完成率；中期目标：承担更多核心任务；长期目标：向管理方向发展");
        return ResponseEntity.ok(ApiResponse.success(suggestion));
    }

    @GetMapping("/divination")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDivination() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        int yearZhi = (year - 4) % 12;
        if (yearZhi < 0) yearZhi += 12;
        int hourZhi = hour / 2 % 12;

        int upperSum = yearZhi + month + day;
        int lowerSum = upperSum + hourZhi;
        int movingLine = (upperSum + lowerSum) % 6;
        if (movingLine == 0) movingLine = 6;

        String[] hexagramNames = {"乾", "坤", "屯", "蒙", "需", "讼", "师", "比"};
        int upperHexagram = upperSum % 8;
        if (upperHexagram == 0) upperHexagram = 8;
        int lowerHexagram = lowerSum % 8;
        if (lowerHexagram == 0) lowerHexagram = 8;

        Map<String, Object> result = new HashMap<>();
        result.put("originalHexagram", hexagramNames[upperHexagram - 1]);
        result.put("changedHexagram", hexagramNames[(upperHexagram % 8)]);
        result.put("suitable", "规划、沟通、执行");
        result.put("avoid", "冲动决策");
        result.put("fortuneLevel", "★★★★ 吉");
        result.put("description", "今日得「" + hexagramNames[upperHexagram - 1] + "」卦，顺应天时，事半功倍！");

        return ResponseEntity.ok(ApiResponse.success(result));
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

    private String generateMbtiReport(String type) {
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

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}