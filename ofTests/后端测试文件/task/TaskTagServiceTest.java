package com.pandora.backend.task;

/**
 * TaskTagService 单元测试 - 任务标签管理模块
 * 
 * 测试目标：验证任务标签的增删改查功能
 * 
 * 测试范围：
 * - 正常场景：标签顺序保留、列表/详情/审核响应
 * - 异常场景：角色越权、停用账号、HTTP权限、名称色板无效、引用失效
 * 
 * 前置条件：使用 SpringJUnitConfig 和 H2 内存数据库
 * 测试用例：9个
 */


import com.pandora.backend.common.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.TestExecutionListeners({
    org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
    org.springframework.test.context.transaction.TransactionalTestExecutionListener.class
})
@SpringJUnitConfig(TaskChangeServiceTest.Config.class)
@Transactional
class TaskTagServiceTest {
    @Autowired JdbcTemplate db;
    @Autowired TaskTagService tags;
    @Autowired TaskChangeService changes;

    @BeforeEach void seed() {
        db.update("DELETE FROM task_tag_links"); db.update("DELETE FROM task_tags");
        db.update("DELETE FROM task_change_requests"); db.update("DELETE FROM tasks");
        db.update("DELETE FROM user_hierarchy"); db.update("DELETE FROM users");
        String[] roles = {"ADMIN", "FOUNDER", "DEPT_HEAD", "TEAM_LEADER", "EMPLOYEE"};
        for (int i = 0; i < roles.length; i++) db.update("INSERT INTO users (id, username, password_hash, role, approval_status) VALUES (?, ?, 'hash', ?, 'APPROVED')", i + 1, "tag-user" + i, roles[i]);
        db.update("INSERT INTO user_hierarchy (superior_id, subordinate_id) VALUES (2,3),(3,4),(4,5)");
    }
    private long number(Map<String, Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    private long tag(String name, int color) { return number(tags.save(2, null, Map.of("name", name, "colorIndex", color)), "id"); }
    private Map<String, Object> create(List<Long> ids) {
        Map<String, Object> data = new HashMap<>(Map.of("name", "带标签任务", "content", "任务正文", "assigneeId", 5L, "tagIds", ids));
        return data;
    }
    private List<Map<String, Object>> attached(long task) {
        Map<String, Object> row = new HashMap<>(Map.of("id", task)); tags.attach(List.of(row));
        return (List<Map<String, Object>>) row.get("tags");
    }

    /**
     * 测试用例：角色等价类越权
     * 输入类别：异类-越权（越权输入）
     * 测试步骤：非FOUNDER尝试管理标签
     * 预期结果：被拒绝，只有FOUNDER可管理
     */
    void onlyFounderCanCreateUpdateOrDeleteEvenAdmin(long actor) {
        long id = tag("合法标签", 0);
        assertThrows(BusinessException.class, () -> tags.save(actor, null, Map.of("name", "越权", "colorIndex", 1)));
        assertThrows(BusinessException.class, () -> tags.save(actor, id, Map.of("name", "篡改", "colorIndex", 2)));
        assertThrows(BusinessException.class, () -> tags.delete(actor, id));
        assertEquals("合法标签", tags.list().get(0).get("name"));
    /**
     * 测试用例：停用账号
     * 输入类别：异类-越权（越权输入）
     * 测试步骤：停用FOUNDER尝试管理标签
     * 预期结果：被拒绝
     */
    @Test void disabledFounderCannotManage() {
        db.update("UPDATE users SET deleted = 1 WHERE id = 2");
        assertThrows(BusinessException.class, () -> tag("失效账号", 0));
    /**
     * 测试用例：名称/色板校验
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入无效的标签名称或色板
     * 预期结果：被拒绝
     */
    @Test void normalizedNamesAndPaletteAreValidated() {
        long id = tag(" ＃Release ", 3);
        assertEquals("Release", tags.list().get(0).get("name"));
        assertThrows(BusinessException.class, () -> tag("release", 1));
        for (String bad : List.of("#", "包含 空格", "a#b", "a\nb", "x".repeat(33))) assertThrows(BusinessException.class, () -> tag(bad, 0));
        assertThrows(BusinessException.class, () -> tag("颜色越界", 8));
        assertThrows(BusinessException.class, () -> tags.save(2, id, Map.of("name", "颜色非整数", "colorIndex", 1.5)));
        assertThrows(BusinessException.class, () -> tags.save(2, id, Map.of("name", "伪造属性", "colorIndex", 0, "owner", 1)));
    /**
     * 测试用例：标签顺序保留
     * 输入类别：同类（有效等价类）
     * 测试步骤：创建、更新、删除标签
     * 预期结果：顺序保留且元数据变更应用到现有任务
     */
    @Test void orderSurvivesCreationAndMetadataChangesApplyToExistingTasks() {
        long red = tag("红色", 1), blue = tag("蓝色", 3);
        long task = number(changes.submit(2, "CREATE", null, create(List.of(blue, red))), "id");
        assertEquals(List.of(blue, red), attached(task).stream().map(t -> number(t, "id")).toList());
        tags.save(2, blue, Map.of("name", "更新后的蓝色", "colorIndex", 4));
        assertEquals("更新后的蓝色", attached(task).get(0).get("name"));
        assertEquals(4, ((Number) attached(task).get(0).get("color_index")).intValue());
        tags.delete(2, blue);
        assertEquals(red, number(attached(task).get(0), "id"));
        tags.delete(2, red); assertTrue(attached(task).isEmpty());
        assertDoesNotThrow(() -> tag("红色", 6));
    /**
     * 测试用例：越权引用
     * 输入类别：异类-越权（越权输入）
     * 测试步骤：尝试通过标签引用绕过任务权限
     * 预期结果：被拒绝
     */
    @Test void referencesAreReviewedAndCannotBypassTaskPermissions() {
        long first = tag("首标签", 0), second = tag("次标签", 2);
        Map<String, Object> result = changes.submit(5, "CREATE", null, create(List.of(second, first)));
        assertEquals("PENDING", result.get("approvalStatus"));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM task_tag_links", Integer.class));
        long task = number(changes.decide(4, number(result, "requestId"), true, ""), "id");
        assertEquals(List.of(second, first), attached(task).stream().map(t -> number(t, "id")).toList());
        Map<String, Object> edit = changes.submit(5, "UPDATE", task, Map.of("tagIds", List.of(first)));
        assertEquals(2, attached(task).size());
        changes.decide(4, number(edit, "requestId"), true, "");
        assertEquals(first, number(attached(task).get(0), "id"));
        assertThrows(BusinessException.class, () -> changes.submit(3, "UPDATE", task, Map.of("tagIds", List.of())));
    /**
     * 测试用例：引用失效
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：删除待审标签后审批
     * 预期结果：阻断审批
     */
    @Test void deletedPendingTagBlocksApprovalWithoutCreatingTask() {
        long id = tag("即将删除", 0);
        Map<String, Object> request = changes.submit(5, "CREATE", null, create(List.of(id)));
        tags.delete(2, id);
        assertThrows(BusinessException.class, () -> changes.decide(4, number(request, "requestId"), true, ""));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM tasks", Integer.class));
        assertEquals("PENDING", db.queryForObject("SELECT status FROM task_change_requests WHERE id = ?", String.class, number(request, "requestId")));
    /**
     * 测试用例：无效引用
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入无效的标签引用
     * 预期结果：被拒绝且保留旧标签
     */
    @Test void invalidReferencesAreRejectedAndOldEditsPreserveTags() {
        long id = tag("保留", 5);
        long task = number(changes.submit(1, "CREATE", null, create(List.of(id))), "id");
        changes.submit(1, "UPDATE", task, Map.of("isUrgent", true));
        assertEquals(id, number(attached(task).get(0), "id"));
        for (Object value : List.of(List.of(id, id), List.of(-1), List.of(987654321), List.of(1.5), List.of("1"), "1", Collections.nCopies(11, id))) {
            assertThrows(BusinessException.class, () -> changes.submit(1, "UPDATE", task, Map.of("tagIds", value)));
        }
        assertThrows(BusinessException.class, () -> changes.submit(1, "UPDATE", task, Map.of("tags", List.of(Map.of("name", "伪造")))));
        changes.submit(1, "UPDATE", task, Map.of("tagIds", List.of()));
        assertTrue(attached(task).isEmpty());
    }

    @AfterEach void clearAuthentication() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    /**
     * 测试用例：HTTP权限
     * 输入类别：异类-越权（越权输入）
     * 测试步骤：非FOUNDER访问HTTP端点
     * 预期结果：强制执行FOUNDER权限
     */
    @Test void httpEndpointsEnforceFounderAndAllowOthersToRead() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new TaskTagController(tags))
                .setControllerAdvice(new com.pandora.backend.common.GlobalExceptionHandler()).build();
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(2L, "", List.of()));
        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/task-tags")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("name", "接口标签", "colorIndex", 3))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long id = json.readTree(result.getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        assertTrue(id > 0);
        for (long actor : List.of(1L, 3L, 4L, 5L)) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, "", List.of()));
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/task-tags"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            for (var request : List.of(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/task-tags"), org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/task-tags/" + id))) {
                mvc.perform(request.contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("name", "越权修改", "colorIndex", 1))))
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
            }
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/task-tags/" + id))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        }
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(2L, "", List.of()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/task-tags/" + id)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("name", "接口改名", "colorIndex", 5))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/task-tags/" + id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        assertTrue(tags.list().isEmpty());
    }
    /**
     * 测试用例：列表/详情/审核响应
     * 输入类别：同类（有效等价类）
     * 测试步骤：获取列表、详情、审核响应
     * 预期结果：包含有序可读标签
     */
    @Test void listDetailAndReviewResponsesIncludeOrderedReadableTags() throws Exception {
        long first = tag("研发", 3), second = tag("发布", 1);
        long task = number(changes.submit(2, "CREATE", null, create(List.of(second, first))), "id");
        TaskController controller = new TaskController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "jdbcTemplate", db);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "taskTags", tags);
        assertEquals(List.of(second, first), ((List<Map<String, Object>>) controller.getTask(task).getBody().getData().get("tags")).stream().map(t -> number(t, "id")).toList());
        List<Map<String, Object>> rows = controller.listTasks(null, 5L, null, true).getBody().getData();
        assertEquals(1, rows.size()); assertEquals(2, ((List<?>) rows.get(0).get("tags")).size());
        Map<String, Object> request = changes.submit(5, "UPDATE", task, Map.of("tagIds", List.of(first, second)));
        assertEquals("#研发  #发布", changes.requests(4, false).get(0).get("tag_names"));
        tags.delete(2, first);
        assertTrue(changes.requests(4, false).get(0).get("tag_names").toString().contains("已删除标签"));
    }
}
