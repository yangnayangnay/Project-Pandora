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
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping("/ai")
public class AIController {

    private static final Logger log = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AIGatewayService aiGatewayService;

    @Autowired
    private LocalFallbackAIClient localFallbackAIClient;

    // Round28 REQ-28-12: MBTI 新报告后失效个人十大 AI 点评缓存
    @Autowired
    private com.pandora.backend.view.PersonalTop10Generator personalTop10Generator;

    @Value("${deepseek.api-url:}")
    private String deepseekApiUrl;

    @PostMapping("/mbti/submit")
    @Auditable(operationType = "AI_MBTI_SUBMIT", targetType = "AI_ANALYSIS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitMbti(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId();
        List<Integer> answers = (List<Integer>) request.get("answers");
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "答题数据不能为空");
        }

        String personalityType = calculateMbtiType(answers);
        String reportContent;
        String aiSource;

        // Round28 REQ-28-02: 同步段解析透传配置，final 捕获传入异步 lambda（异步段不可用请求上下文）
        final AiTransmitConfig aiConfig = AiTransmitConfig.fromHeaders(servletRequest);

        try {
            // Round24 T2.4: 15 秒限时（消灭无限等待；超时走本地报告兜底）
            java.util.concurrent.CompletableFuture<Map<String, Object>> aiFuture =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> aiGatewayService.invoke(
                            "请为MBTI性格类型 " + personalityType + " 生成一份详细的性格分析报告，包括性格特点、优势劣势和适合的职业方向。",
                            "MBTI_REPORT", aiConfig));
            Map<String, Object> aiResult = aiFuture.get(15, java.util.concurrent.TimeUnit.SECONDS);
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

        // Round28 REQ-28-12: 新 MBTI 报告落库后失效个人十大（下次查看重新生成点评）
        try {
            personalTop10Generator.invalidateUser(userId);
        } catch (Exception ignored) {}

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
            // Round24 T5.1: 响应键驼峰化（与 /ai/mbti/report/generate 响应键对齐，App 端无需双键兜底）
            return ResponseEntity.ok(ApiResponse.success(formatReport(report)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无MBTI报告");
        }
    }

    /**
     * Round24 T5.1: mbti_reports 原始行（大写下划线键）→ 驼峰单键结构。
     */
    private Map<String, Object> formatReport(Map<String, Object> row) {
        Map<String, Object> result = new HashMap<>();
        result.put("reportId", getRowValue(row, "id", "ID"));
        result.put("personalityType", getRowValue(row, "personality_type", "PERSONALITY_TYPE"));
        result.put("reportContent", getRowValue(row, "report_content", "REPORT_CONTENT"));
        result.put("generatedAt", formatTimestamp(getRowValue(row, "generated_at", "GENERATED_AT")));
        return result;
    }

    private Object getRowValue(Map<String, Object> row, String lower, String upper) {
        Object val = row.get(lower);
        if (val == null) val = row.get(upper);
        return val;
    }

    private String formatTimestamp(Object val) {
        if (val == null) return null;
        if (val instanceof java.sql.Timestamp) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((java.sql.Timestamp) val);
        }
        return val.toString();
    }

    @PostMapping("/mbti/report/generate")
    @Auditable(operationType = "AI_MBTI_REPORT_GENERATE", targetType = "AI_ANALYSIS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateMbtiReport(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId();
        String personalityType = request.get("personalityType") != null ? request.get("personalityType").toString() : null;
        if (personalityType == null || !personalityType.matches("^[EI][SN][TF][JP]$")) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "性格类型不合法，须为^[EI][SN][TF][JP]$格式");
        }

        Object answersObj = request.get("answers");
        String answersStr = answersObj != null ? answersObj.toString() : null;

        String reportContent;
        String aiSource;

        // Round28 REQ-28-02: 同步段解析透传配置，final 捕获传入异步 lambda
        final AiTransmitConfig aiConfig = AiTransmitConfig.fromHeaders(servletRequest);

        try {
            // Round24 T2.4: 15 秒限时（消灭无限等待；超时走本地报告兜底）
            java.util.concurrent.CompletableFuture<Map<String, Object>> aiFuture =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> aiGatewayService.invoke(
                            "请为MBTI性格类型 " + personalityType + " 生成一份详细的性格分析报告，包括性格特点、优势劣势和适合的职业方向。",
                            "MBTI_REPORT", aiConfig));
            Map<String, Object> aiResult = aiFuture.get(15, java.util.concurrent.TimeUnit.SECONDS);
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

        // Round28 REQ-28-12: 新 MBTI 报告落库后失效个人十大（下次查看重新生成点评）
        try {
            personalTop10Generator.invalidateUser(userId);
        } catch (Exception ignored) {}

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> keywordAnalysis(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        String text = (String) request.get("text");
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "分析文本不能为空");
        }

        List<Map<String, Object>> keywords;
        String aiSource;

        // Round28 REQ-28-02: 同步调用直接透传
        AiTransmitConfig aiConfig = AiTransmitConfig.fromHeaders(servletRequest);

        try {
            Map<String, Object> aiResult = aiGatewayService.invoke(
                    "分析以下文本的10个关键词及其出现次数，以JSON数组格式返回：\n" + text,
                    "KEYWORD_ANALYSIS", aiConfig);
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuggestion(HttpServletRequest servletRequest) {
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
            // Round28 REQ-28-02: 同步段解析透传配置，final 捕获传入异步 lambda
            final AiTransmitConfig aiConfig = AiTransmitConfig.fromHeaders(servletRequest);
            java.util.concurrent.CompletableFuture<Map<String, Object>> aiFuture =
                    java.util.concurrent.CompletableFuture.supplyAsync(() ->
                            aiGatewayService.invoke(
                                    "用户角色：" + fRoleDisplay + "，任务总数：" + fTotalTasks + "，已完成：" + fCompletedTasks +
                                    "，进行中：" + fInProgressTasks + "，逾期：" + fOverdueTasks + "，日志数：" + fLogCount +
                                    "。请生成工作建议、职业发展建议和发展计划。",
                                    "SUGGESTION", aiConfig));
            // SUGGESTION max_tokens=500，本地 GPU 推理需 8~10 秒，5 秒限时会导致永远走本地兜底（实测推翻设计假设）；
            // 与 MBTI（2.4.5-D）同理放宽至 15 秒，保证 REQ-24-07"建议真实 AI 生成"可达；App 端已有加载态承载等待
            Map<String, Object> aiResult = aiFuture.get(15, java.util.concurrent.TimeUnit.SECONDS);
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

    /**
     * Round23 REQ-23-08: 梅花易数卦象 AI 解读接口。
     * 入参: {originalName(必填), mutualName/changedName(可选), movingLine(1~6, 可选)}
     * 出参: {analysis(非空), source: EXTERNAL|LOCAL_FALLBACK}
     * AI 不可用（含 5 秒超时）时降级为本地兜底文案返回 200，不向上抛 AI_UNAVAILABLE。
     * Round24 T4.2: 解读生成逻辑提取为 generateDivinationAnalysis 公共方法（与 /ai/divination/record 共用）。
     */
    @PostMapping("/divination")
    @Auditable(operationType = "AI_DIVINATION_ANALYSIS", targetType = "AI_ANALYSIS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> divinationAnalysis(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        String originalName = (String) request.get("originalName");
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "本卦名称不能为空");
        }

        String mutualName = strOrEmpty(request.get("mutualName"));
        String changedName = strOrEmpty(request.get("changedName"));
        Integer movingLine = parseMovingLine(request.get("movingLine"));

        // Round28 REQ-28-07: 入口解析透传配置传入共用生成方法（起卦后 AI 分析展示）
        AiTransmitConfig aiConfig = AiTransmitConfig.fromHeaders(servletRequest);
        Map<String, Object> result = generateDivinationAnalysis(originalName, mutualName, changedName, movingLine, aiConfig);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Round24 T4.3: 查询当前用户当日梅花易数记录（每日一次约束的回显入口）。
     * 有记录: {exists:true, record:{originalName, mutualName, changedName, movingLine, analysis, source, divinationDate}}
     * 无记录/查询异常: {exists:false}（200 不抛 404——宁可放行不可误拦）
     */
    @GetMapping("/divination/today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDivinationToday() {
        Long userId = getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        result.put("exists", false);
        try {
            Map<String, Object> row = findTodayDivinationRecord(userId);
            if (row != null) {
                result.put("exists", true);
                result.put("record", formatDivinationRecord(row));
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Round24 T4.4: 起卦收口接口（服务端生成解读并落库，每日一次幂等锁定）。
     * 入参: {originalName(必填), mutualName/changedName/movingLine(可选, 仅 1~6 整数)}
     * 出参: {alreadyToday, record{...含 analysis/source/divinationDate}}
     * 先查当日记录（有→alreadyToday=true+已存记录）→ 无→generateDivinationAnalysis→INSERT→alreadyToday=false+新记录。
     * UNIQUE 冲突（并发窗口）→ catch 后改查当日记录返回 alreadyToday=true；
     * 非 UNIQUE 落库失败 → 记日志 + 仍返回当次解读（alreadyToday=false 放行语义，下次可重试）。
     */
    @PostMapping("/divination/record")
    @Auditable(operationType = "AI_DIVINATION_RECORD", targetType = "AI_ANALYSIS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordDivination(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId();
        String originalName = request.get("originalName") != null ? request.get("originalName").toString().trim() : "";
        if (originalName.isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_REQUIRED_FIELD, "本卦名称不能为空");
        }
        String mutualName = strOrEmpty(request.get("mutualName"));
        String changedName = strOrEmpty(request.get("changedName"));
        Integer movingLine = parseMovingLine(request.get("movingLine"));

        // 幂等锁定：当日已有记录 → 直接返回已存记录
        Map<String, Object> existing = findTodayDivinationRecord(userId);
        if (existing != null) {
            return ResponseEntity.ok(ApiResponse.success(buildDivinationResponse(true, formatDivinationRecord(existing))));
        }

        // 服务端生成解读（复用 /ai/divination 提示词 + 5 秒限时 + 带上下文兜底）
        // Round28 REQ-28-07: 透传配置解析后传入（起卦后 AI 分析展示）
        AiTransmitConfig aiConfig = AiTransmitConfig.fromHeaders(servletRequest);
        Map<String, Object> generated = generateDivinationAnalysis(originalName, mutualName, changedName, movingLine, aiConfig);
        String analysis = (String) generated.get("analysis");
        String source = (String) generated.get("source");

        Map<String, Object> record = new HashMap<>();
        record.put("originalName", originalName);
        record.put("mutualName", mutualName.isEmpty() ? null : mutualName);
        record.put("changedName", changedName.isEmpty() ? null : changedName);
        record.put("movingLine", movingLine);
        record.put("analysis", analysis);
        record.put("source", source);
        record.put("divinationDate", java.time.LocalDate.now().toString());

        try {
            jdbcTemplate.update(
                    "INSERT INTO divination_records (user_id, divination_date, original_name, mutual_name, changed_name, moving_line, analysis, source) "
                            + "VALUES (?, CURRENT_DATE, ?, ?, ?, ?, ?, ?)",
                    userId, originalName,
                    mutualName.isEmpty() ? null : mutualName,
                    changedName.isEmpty() ? null : changedName,
                    movingLine, analysis, source);
        } catch (Exception e) {
            // 并发窗口兜底：UNIQUE 冲突 → 改查当日记录返回 alreadyToday=true
            Map<String, Object> concurrent = findTodayDivinationRecord(userId);
            if (concurrent != null) {
                return ResponseEntity.ok(ApiResponse.success(buildDivinationResponse(true, formatDivinationRecord(concurrent))));
            }
            // 非 UNIQUE 落库失败（或冲突后仍查不到）：记日志 + 仍返回当次解读（放行语义）
            log.warn("起卦记录落库失败: {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(buildDivinationResponse(false, record)));
    }

    /**
     * Round24 T4.2: 梅花易数解读生成公共方法（/ai/divination 与 /ai/divination/record 共用）。
     * 提示词组装（积极红线+本卦/互卦/变卦+动爻）→ CompletableFuture 5 秒限时 → invoke →
     * 非外部结果（超时/异常/通道降级/空结果）时走带卦象上下文的本地兜底（48 组差异化模板）。
     */
    private Map<String, Object> generateDivinationAnalysis(String originalName, String mutualName, String changedName, Integer movingLine, AiTransmitConfig aiConfig) {
        // 积极红线提示词：鼓励正向行动建议，禁止消极/恐吓/宿命论表述
        StringBuilder promptBuilder = new StringBuilder()
                .append("请以积极向上、鼓励正向的基调解读以下梅花易数卦象，给出行动建议与鼓励，篇幅100~200字；")
                .append("禁止出现'凶兆''大凶''不可为'等消极、恐吓、宿命论表述：")
                .append("本卦").append(originalName.trim());
        if (mutualName != null && !mutualName.isEmpty()) {
            promptBuilder.append("，互卦").append(mutualName);
        }
        if (changedName != null && !changedName.isEmpty()) {
            promptBuilder.append("，变卦").append(changedName);
        }
        if (movingLine != null) {
            promptBuilder.append("，动爻第").append(movingLine).append("爻");
        }
        final String prompt = promptBuilder.toString();

        // Round24 T3.2: 卦象上下文（本地兜底 48 组差异化模板入参）
        Map<String, Object> context = new HashMap<>();
        context.put("originalName", originalName.trim());
        if (mutualName != null && !mutualName.isEmpty()) {
            context.put("mutualName", mutualName);
        }
        if (changedName != null && !changedName.isEmpty()) {
            context.put("changedName", changedName);
        }
        if (movingLine != null) {
            context.put("movingLine", movingLine);
        }

        String analysis = null;
        String source = "LOCAL_FALLBACK";
        try {
            java.util.concurrent.CompletableFuture<Map<String, Object>> aiFuture =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> aiGatewayService.invoke(prompt, "DIVINATION", aiConfig));
            // Round25 缺陷⑪: 5s 限时低于本地 GPU Ollama 热态推理耗时（8~10s）致永远走本地兜底，同 281 行 SUGGESTION 先例放宽至 15s
            Map<String, Object> aiResult = aiFuture.get(15, java.util.concurrent.TimeUnit.SECONDS);
            Object resultText = aiResult.get("result");
            Object sourceObj = aiResult.get("source");
            String aiSource = sourceObj != null ? sourceObj.toString() : "";
            // 仅外部结果直接采用；通道降级返回的固定兜底文案不含卦象上下文，改走下方带上下文模板
            if ("EXTERNAL".equals(aiSource) && resultText != null && !resultText.toString().trim().isEmpty()) {
                analysis = resultText.toString().trim();
                source = "EXTERNAL";
            }
        } catch (Exception ignored) {}

        // AI 不可用（超时/异常/通道降级/空结果）→ 带卦象上下文的本地兜底文案，200 响应，不向上抛 AI_UNAVAILABLE
        if (analysis == null || analysis.trim().isEmpty()) {
            try {
                analysis = localFallbackAIClient.call(prompt, "DIVINATION", context);
            } catch (Exception e) {
                analysis = "卦象解读：卦象蕴含积极向上的能量，建议您保持乐观心态、专注核心目标，"
                        + "顺势而为、主动沟通，您的努力正被看见，好消息正在靠近。请理性看待占卜结果，仅供娱乐与文化体验。";
            }
            source = "LOCAL_FALLBACK";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("analysis", analysis);
        result.put("source", source);
        return result;
    }

    /** 当日起卦记录查询（user_id + CURRENT_DATE，0 点自然重置语义） */
    private Map<String, Object> findTodayDivinationRecord(Long userId) {
        try {
            return jdbcTemplate.queryForMap(
                    "SELECT * FROM divination_records WHERE user_id = ? AND divination_date = CURRENT_DATE", userId);
        } catch (Exception e) {
            return null;
        }
    }

    /** divination_records 原始行 → 驼峰契约结构 */
    private Map<String, Object> formatDivinationRecord(Map<String, Object> row) {
        Map<String, Object> record = new HashMap<>();
        record.put("originalName", getRowValue(row, "original_name", "ORIGINAL_NAME"));
        record.put("mutualName", getRowValue(row, "mutual_name", "MUTUAL_NAME"));
        record.put("changedName", getRowValue(row, "changed_name", "CHANGED_NAME"));
        record.put("movingLine", getRowValue(row, "moving_line", "MOVING_LINE"));
        record.put("analysis", getRowValue(row, "analysis", "ANALYSIS"));
        record.put("source", getRowValue(row, "source", "SOURCE"));
        Object date = getRowValue(row, "divination_date", "DIVINATION_DATE");
        record.put("divinationDate", date != null ? date.toString() : null);
        return record;
    }

    private Map<String, Object> buildDivinationResponse(boolean alreadyToday, Map<String, Object> record) {
        Map<String, Object> result = new HashMap<>();
        result.put("alreadyToday", alreadyToday);
        result.put("record", record);
        return result;
    }

    /** movingLine 可选解析：仅接受 1~6 的整数，其余情况视为未提供（null） */
    private Integer parseMovingLine(Object movingLineObj) {
        if (movingLineObj instanceof Number) {
            int line = ((Number) movingLineObj).intValue();
            if (line >= 1 && line <= 6) {
                return line;
            }
        }
        return null;
    }

    private String strOrEmpty(Object value) {
        if (value == null) return "";
        String str = value.toString().trim();
        return str;
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

        // Round27 REQ-27-11: 60 题按题库 dimension 分桶计分（与 App 端逐题对齐）；
        // 越界或非法维度回退 idx%4 旧序，保持兼容
        for (int idx = 0; idx < answers.size(); idx++) {
            int ans = answers.get(idx);
            String dim = MbtiQuestionBank.dimensionOf(idx);
            if ("EI".equals(dim)) { if (ans == 1) e++; else i++; }
            else if ("SN".equals(dim)) { if (ans == 1) s++; else n++; }
            else if ("TF".equals(dim)) { if (ans == 1) t++; else f++; }
            else if ("JP".equals(dim)) { if (ans == 1) j++; else p++; }
            else {
                log.warn("MBTI dimension fallback at idx={}, dim={}, answersSize={}", idx, dim, answers.size());
                if (idx % 4 == 0) { if (ans == 1) e++; else i++; }
                else if (idx % 4 == 1) { if (ans == 1) s++; else n++; }
                else if (idx % 4 == 2) { if (ans == 1) t++; else f++; }
                else { if (ans == 1) j++; else p++; }
            }
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