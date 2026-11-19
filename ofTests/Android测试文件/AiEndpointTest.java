/**
 * AI端点测试 - 测试网关保留、路径规范化、凭据拒绝等
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/core\network\AiEndpointTest.java
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

package com.example.project_pandora.core.network;

import org.junit.Test;
import static org.junit.Assert.*;

public class AiEndpointTest {
    @Test public void originAndVersionedBaseBecomeUsableChatEndpoints() {
        assertEquals("https://api.deepseek.com/chat/completions", AiEndpoint.chat(" https://api.deepseek.com/ "));
        assertEquals("https://example.test/v1/chat/completions", AiEndpoint.chat("https://example.test/v1/"));
        assertEquals("http://localhost:11434/v1/chat/completions", AiEndpoint.chat("http://localhost:11434/v1"));
        assertEquals("https://api.openai.com/v1/chat/completions", AiEndpoint.chat("https://api.openai.com"));
    }
    @Test public void explicitGatewaysAndQueriesArePreserved() {
        String custom = "https://gateway.test/tenant/chat/completions?api-version=2026-01";
        assertEquals(custom, AiEndpoint.chat(custom));
        assertEquals("https://gateway.test/tenant/models?api-version=2026-01", AiEndpoint.models(custom));
        assertEquals("https://api.deepseek.com/models", AiEndpoint.models("https://api.deepseek.com"));
        assertEquals("https://gateway.test/custom", AiEndpoint.chat("https://gateway.test/custom"));
    }
    @Test public void rejectsCredentialsFragmentsAndNonHttpSchemes() {
        for (String invalid : new String[]{"", "file:///tmp/key", "https://user:password@host.test", "https://host.test/#fragment", "https:///missing-host"}) {
            try { AiEndpoint.chat(invalid); fail(invalid); } catch (IllegalArgumentException expected) {}
        }
    }
}
