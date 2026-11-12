package com.pandora.backend.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiEndpointTest {
    @Test void legacyRootConfigurationUsesTheSameChatEndpointAsThePhone() {
        assertEquals("https://api.deepseek.com/chat/completions", AiEndpoint.chat("https://api.deepseek.com/"));
        assertEquals("https://host.test/v1/chat/completions", AiEndpoint.chat("https://host.test/v1"));
    }
    @Test void customEndpointIsPreservedAndMalformedEndpointRejected() {
        assertEquals("https://host.test/provider/chat/completions?version=2", AiEndpoint.chat("https://host.test/provider/chat/completions?version=2"));
        assertThrows(IllegalArgumentException.class, () -> AiEndpoint.chat("file:///data"));
    }
}
