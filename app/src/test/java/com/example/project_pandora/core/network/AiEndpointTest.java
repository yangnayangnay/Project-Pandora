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
