package com.tamabee.api_hr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Cấu hình WebSocket cho hệ thống thông báo real-time.
 * Sử dụng STOMP protocol với SockJS fallback.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * Cấu hình message broker cho WebSocket.
     * - /topic: Broadcast messages đến nhiều subscribers
     * - /queue: Point-to-point messages đến một user cụ thể
     * - /user: Prefix cho user-specific destinations
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Tạo TaskScheduler cho SimpleBroker
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.initialize();

        // Cấu hình simple broker với TaskScheduler
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler);

        // Prefix cho messages từ client đến server
        config.setApplicationDestinationPrefixes("/app");

        // Prefix cho user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Đăng ký STOMP endpoint cho WebSocket connections.
     * Endpoint /ws/notifications với SockJS fallback.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Cấu hình interceptor cho inbound channel.
     * Đăng ký WebSocketAuthInterceptor để xác thực JWT khi CONNECT.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
