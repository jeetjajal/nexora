package com.nexora.phase17;

import com.nexora.websocket.config.WebSocketConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Phase 17 - WebSocketConfig Unit Tests")
class WebSocketConfigTest {

    @Test
    @DisplayName("Should configure message broker prefixes correctly")
    void testConfigureMessageBroker() {
        WebSocketConfig config = new WebSocketConfig();
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic", "/queue");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setUserDestinationPrefix("/user");
    }

    @Test
    @DisplayName("Should register STOMP endpoints with SockJS")
    void testRegisterStompEndpoints() {
        WebSocketConfig config = new WebSocketConfig();
        ReflectionTestUtils.setField(config, "websocketEndpoint", "/ws-nexora");
        ReflectionTestUtils.setField(config, "allowedOrigins", "*");

        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);

        when(registry.addEndpoint(eq("/ws-nexora"))).thenReturn(registration);
        when(registration.setAllowedOriginPatterns(any(String[].class))).thenReturn(registration);

        config.registerStompEndpoints(registry);

        verify(registry, atLeastOnce()).addEndpoint("/ws-nexora");
        verify(registration).withSockJS();
    }
}
