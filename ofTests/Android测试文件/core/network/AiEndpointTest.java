package com.example.project_pandora.core.network;

/**
 * AiEndpoint 单元测试 - AI端点配置模块
 * 
 * 测试目标：验证AI端点配置
 * 
 * 测试范围：
 * - 正常场景：网关保留、路径规范化
 * - 异常场景：凭据片段/非http拒绝
 * 
 * 测试用例：3个
 */


import org.junit.Test;
import static org.junit.Assert.*;

    /**
     * 测试用例：路径规范化
     * 输入类别：同类（有效等价类）
     * 测试步骤：原始和版本化基础变为可用聊天端点
     * 预期结果：正确生成端点URL
     */
    @Test public void originAndVersionedBaseBecomeUsableChatEndpoints() {
        assertEquals("https://api.deepseek.com/chat/completions", AiEndpoint.chat(" https://api.deepseek.com/ "));
        assertEquals("https://example.test/v1/chat/completions", AiEndpoint.chat("https://example.test/v1/"));
        assertEquals("http://localhost:11434/v1/chat/completions", AiEndpoint.chat("http://localhost:11434/v1"));
        assertEquals("https://api.openai.com/v1/chat/completions", AiEndpoint.chat("https://api.openai.com"));
    /**
     * 测试用例：显式网关保留
     * 输入类别：同类（有效等价类）
     * 测试步骤：显式网关和查询被保留
     * 预期结果：正确保留
     */
    @Test public void explicitGatewaysAndQueriesArePreserved() {
        String custom = "https://gateway.test/tenant/chat/completions?api-version=2026-01";
        assertEquals(custom, AiEndpoint.chat(custom));
        assertEquals("https://gateway.test/tenant/models?api-version=2026-01", AiEndpoint.models(custom));
        assertEquals("https://api.deepseek.com/models", AiEndpoint.models("https://api.deepseek.com"));
        assertEquals("https://gateway.test/custom", AiEndpoint.chat("https://gateway.test/custom"));
    /**
     * 测试用例：凭据片段/非http拒绝
     * 输入类别：异类-异常（无效等价类）
     * 测试步骤：传入无效URL
     * 预期结果：被拒绝
     */
    @Test public void rejectsCredentialsFragmentsAndNonHttpSchemes() {
        for (String invalid : new String[]{"", "file:///tmp/key", "https://user:password@host.test", "https://host.test/#fragment", "https:///missing-host"}) {
            try { AiEndpoint.chat(invalid); fail(invalid); } catch (IllegalArgumentException expected) {}
        }
    }
}
