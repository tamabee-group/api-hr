package com.tamabee.api_hr.config;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.tamabee.api_hr.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor xác thực JWT cho WebSocket connections.
 * Validate JWT token khi client gửi STOMP CONNECT command.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Lấy token từ Authorization header
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = extractToken(authHeader);

            if (token == null) {
                log.warn("WebSocket CONNECT: Không tìm thấy JWT token");
                throw new MessageDeliveryException("Không có token xác thực");
            }

            // Validate JWT token
            Map<String, Object> claims = jwtUtil.validateToken(token);

            if (claims == null) {
                log.warn("WebSocket CONNECT: JWT token không hợp lệ");
                throw new MessageDeliveryException("Token không hợp lệ hoặc đã hết hạn");
            }

            // Lấy thông tin user từ claims
            Long userId = getLongClaim(claims, "userId");
            String email = (String) claims.get("sub");
            String role = (String) claims.get("role");

            if (userId == null || email == null || role == null) {
                log.warn("WebSocket CONNECT: JWT claims thiếu thông tin cần thiết");
                throw new MessageDeliveryException("Token thiếu thông tin xác thực");
            }

            log.info("WebSocket CONNECT: Xác thực thành công cho user {} (ID: {})", email, userId);

            // Tạo Principal với userId làm name để sử dụng cho user-specific messages
            Principal principal = createPrincipal(userId, email, role);
            accessor.setUser(principal);
        }

        return message;
    }

    /**
     * Trích xuất token từ Authorization header.
     * Hỗ trợ cả format "Bearer <token>" và token trực tiếp.
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }

        // Loại bỏ prefix "Bearer " nếu có
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return authHeader;
    }

    /**
     * Lấy Long value từ claims (hỗ trợ cả Integer và Long).
     */
    private Long getLongClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return null;
    }

    /**
     * Tạo Principal cho WebSocket session.
     * Sử dụng userId làm name để Spring có thể route messages đến đúng user.
     */
    private Principal createPrincipal(Long userId, String email, String role) {
        var authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role));

        // Sử dụng userId làm principal name để dễ dàng gửi messages đến user cụ thể
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId.toString(), // Principal name = userId
                null,
                authorities);

        // Lưu thêm thông tin email vào details nếu cần
        authentication.setDetails(email);

        return authentication;
    }
}
