package com.tamabee.api_hr.service.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.dto.response.NotificationResponse;
import com.tamabee.api_hr.entity.core.NotificationEntity;
import com.tamabee.api_hr.enums.NotificationType;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.core.NotificationMapper;
import com.tamabee.api_hr.repository.core.NotificationRepository;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation của INotificationService.
 * Quản lý thông báo real-time trong hệ thống, hỗ trợ tạo, đọc, đánh dấu đã đọc
 * và push thông báo qua WebSocket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;
    private final ObjectMapper objectMapper;
    private final TenantDataSourceManager tenantDataSourceManager;

    // WebSocket destination cho user notifications
    private static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";
    
    // Tamabee tenant domain
    private static final String TAMABEE_TENANT = "tamabee";

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id, Long userId) {
        NotificationEntity notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> NotFoundException.notification(id));
        return notificationMapper.toResponse(notification);
    }

    // ==================== Mutation Operations ====================

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        NotificationEntity notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> NotFoundException.notification(notificationId));

        notification.setIsRead(true);
        notificationRepository.save(notification);

        log.debug("Đã đánh dấu thông báo {} là đã đọc cho user {}", notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        int updatedCount = notificationRepository.markAllAsReadByUserId(userId);
        log.debug("Đã đánh dấu {} thông báo là đã đọc cho user {}", updatedCount, userId);
    }

    // ==================== Creation Operations ====================

    @Override
    @Transactional
    public void createNotification(Long userId, String code, Map<String, Object> params,
                                   String targetUrl, NotificationType type) {
        // Tạo entity mới
        NotificationEntity notification = NotificationEntity.builder()
                .userId(userId)
                .code(code)
                .params(serializeParams(params))
                .targetUrl(targetUrl)
                .type(type)
                .isRead(false)
                .build();

        // Lưu vào database
        NotificationEntity savedNotification = notificationRepository.save(notification);

        log.debug("Đã tạo thông báo {} cho user {}: code={}", savedNotification.getId(), userId, code);

        // Push qua WebSocket SAU KHI transaction commit để frontend fetch được data mới nhất
        NotificationResponse response = notificationMapper.toResponse(savedNotification);
        pushAfterCommit(userId, response);
    }

    @Override
    @Transactional
    public void createBulkNotifications(List<Long> userIds, String code, Map<String, Object> params,
                                        String targetUrl, NotificationType type) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("Danh sách userIds rỗng, không tạo thông báo");
            return;
        }

        String paramsJson = serializeParams(params);

        // Thu thập responses để push sau khi commit
        List<Map.Entry<Long, NotificationResponse>> pendingPushes = new ArrayList<>();

        // Tạo notifications cho tất cả users
        for (Long userId : userIds) {
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(userId)
                    .code(code)
                    .params(paramsJson)
                    .targetUrl(targetUrl)
                    .type(type)
                    .isRead(false)
                    .build();

            NotificationEntity savedNotification = notificationRepository.save(notification);
            NotificationResponse response = notificationMapper.toResponse(savedNotification);
            pendingPushes.add(Map.entry(userId, response));
        }

        // Push qua WebSocket SAU KHI transaction commit
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Map.Entry<Long, NotificationResponse> entry : pendingPushes) {
                        pushNotification(entry.getKey(), entry.getValue());
                    }
                }
            });
        } else {
            // Không có transaction active, push ngay
            for (Map.Entry<Long, NotificationResponse> entry : pendingPushes) {
                pushNotification(entry.getKey(), entry.getValue());
            }
        }

        log.debug("Đã tạo {} thông báo bulk: code={}", userIds.size(), code);
    }

    @Override
    public void notifyTamabeeStaff(String code, Map<String, Object> params,
                                   String targetUrl, NotificationType type) {
        try {
            // Lấy DataSource của tenant tamabee trực tiếp
            DataSource tamabeeDataSource = tenantDataSourceManager.getDataSource(TAMABEE_TENANT);
            if (tamabeeDataSource == null) {
                log.error("Không tìm thấy DataSource cho tenant tamabee");
                return;
            }
            
            // Tạo JdbcTemplate với DataSource của tamabee
            JdbcTemplate tamabeeJdbcTemplate = new JdbcTemplate(tamabeeDataSource);
            
            // Query trực tiếp danh sách Tamabee admin và manager
            String selectSql = """
                SELECT id FROM users 
                WHERE role IN ('ADMIN_TAMABEE', 'MANAGER_TAMABEE') 
                AND deleted = false
                """;
            
            List<Long> staffIds = tamabeeJdbcTemplate.queryForList(selectSql, Long.class);
            
            if (staffIds.isEmpty()) {
                log.warn("Không tìm thấy Tamabee staff để gửi thông báo");
                return;
            }
            
            log.info("Tìm thấy {} Tamabee staff để gửi thông báo: {}", staffIds.size(), staffIds);
            
            // Insert notifications trực tiếp vào tamabee tenant DB
            String paramsJson = serializeParams(params);
            
            for (Long userId : staffIds) {
                insertNotificationWithJdbc(tamabeeJdbcTemplate, userId, code, paramsJson, targetUrl, type, params);
            }
            
            log.info("Đã gửi thông báo {} cho {} Tamabee staff", code, staffIds.size());
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo cho Tamabee staff: {}", e.getMessage(), e);
        }
    }

    @Override
    public void createNotificationForTenant(String tenantDomain, Long userId, String code,
                                            Map<String, Object> params, String targetUrl,
                                            NotificationType type) {
        try {
            DataSource tenantDataSource = tenantDataSourceManager.getDataSource(tenantDomain);
            if (tenantDataSource == null) {
                log.error("Không tìm thấy DataSource cho tenant: {}", tenantDomain);
                return;
            }

            JdbcTemplate tenantJdbcTemplate = new JdbcTemplate(tenantDataSource);
            String paramsJson = serializeParams(params);
            
            insertNotificationWithJdbc(tenantJdbcTemplate, userId, code, paramsJson, targetUrl, type, params);
            
            log.info("Đã tạo notification {} cho user {} trong tenant {}", code, userId, tenantDomain);
        } catch (Exception e) {
            log.error("Lỗi khi tạo notification cho tenant {}: {}", tenantDomain, e.getMessage(), e);
        }
    }

    /**
     * Insert notification bằng JDBC và push qua WebSocket
     */
    private void insertNotificationWithJdbc(JdbcTemplate jdbcTemplate, Long userId, String code,
                                            String paramsJson, String targetUrl, 
                                            NotificationType type, Map<String, Object> params) {
        try {
            String insertSql = """
                INSERT INTO notifications (user_id, code, params, target_url, type, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, false, NOW())
                RETURNING id
                """;
            
            Long notificationId = jdbcTemplate.queryForObject(
                    insertSql, Long.class, userId, code, paramsJson, targetUrl, type.name());
            
            log.info("Đã insert notification {} vào DB cho user {}", notificationId, userId);
            
            // Push qua WebSocket
            NotificationResponse response = NotificationResponse.builder()
                    .id(notificationId)
                    .code(code)
                    .params(params)
                    .targetUrl(targetUrl)
                    .type(type)
                    .isRead(false)
                    .build();
            pushNotification(userId, response);
            
            log.debug("Đã tạo notification {} cho user {}", notificationId, userId);
        } catch (Exception e) {
            log.error("Lỗi khi insert notification cho user {}: {}", userId, e.getMessage());
        }
    }

    // ==================== WebSocket Operations ====================

    /**
     * Đăng ký push WebSocket sau khi transaction commit.
     * Đảm bảo frontend fetch được data mới nhất khi nhận notification.
     */
    private void pushAfterCommit(Long userId, NotificationResponse response) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pushNotification(userId, response);
                }
            });
        } else {
            // Không có transaction active, push ngay
            pushNotification(userId, response);
        }
    }

    @Override
    public void pushNotification(Long userId, NotificationResponse notification) {
        try {
            // Log tất cả connected users để debug
            int totalUsers = simpUserRegistry.getUserCount();
            log.info("Push notification {} đến user {}: totalConnectedUsers={}", 
                    notification.getId(), userId, totalUsers);
            
            // Log chi tiết các users đang connected
            simpUserRegistry.getUsers().forEach(u -> {
                log.info("Connected user: name={}, sessions={}", u.getName(), u.getSessions().size());
            });
            
            // Kiểm tra user có connected không
            var user = simpUserRegistry.getUser(userId.toString());
            boolean isUserConnected = user != null && !user.getSessions().isEmpty();
            
            log.info("Target user {} connected={}, sessions={}", 
                    userId, isUserConnected,
                    user != null ? user.getSessions().size() : 0);
            
            // Luôn gửi message, Spring sẽ tự xử lý nếu user không connected
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    USER_NOTIFICATION_DESTINATION,
                    notification
            );

            log.info("Đã push thông báo {} qua WebSocket cho user {}", notification.getId(), userId);
        } catch (Exception e) {
            // Log lỗi nhưng không throw exception để không ảnh hưởng đến flow chính
            log.warn("Không thể push thông báo qua WebSocket cho user {}: {}", userId, e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Serialize params Map thành JSON string.
     * Trả về null nếu params là null hoặc rỗng.
     */
    private String serializeParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.warn("Không thể serialize params thành JSON: {}", e.getMessage());
            return null;
        }
    }
}
