package com.tamabee.api_hr.service.admin.impl;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.constants.NotificationCode;
import com.tamabee.api_hr.datasource.TenantDataSourceManager;
import com.tamabee.api_hr.dto.request.CreateSystemNotificationRequest;
import com.tamabee.api_hr.dto.response.NotificationResponse;
import com.tamabee.api_hr.dto.response.SystemNotificationResponse;
import com.tamabee.api_hr.entity.core.SystemNotificationEntity;
import com.tamabee.api_hr.enums.NotificationType;
import com.tamabee.api_hr.enums.TargetAudience;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.admin.SystemNotificationMapper;
import com.tamabee.api_hr.repository.core.SystemNotificationRepository;
import com.tamabee.api_hr.service.admin.interfaces.ISystemNotificationService;
import com.tamabee.api_hr.service.core.interfaces.INotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation của ISystemNotificationService.
 * Quản lý thông báo hệ thống: tạo master copy, gửi cross-tenant, xem danh sách và chi tiết.
 * 
 * Luồng gửi notification:
 * 1. Lưu master copy vào system_notifications table (master DB)
 * 2. Lặp qua tất cả tenant bằng TenantDataSourceManager
 * 3. Query users theo target_audience
 * 4. Chọn title/content theo region user
 * 5. Insert notification bằng JDBC
 * 6. Push WebSocket real-time
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemNotificationServiceImpl implements ISystemNotificationService {

    private final SystemNotificationRepository systemNotificationRepository;
    private final SystemNotificationMapper systemNotificationMapper;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final INotificationService notificationService;

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<SystemNotificationResponse> getAll(Pageable pageable) {
        return systemNotificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(systemNotificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemNotificationResponse getById(Long id) {
        SystemNotificationEntity entity = systemNotificationRepository.findById(id)
                .orElseThrow(() -> NotFoundException.systemNotification(id));
        return systemNotificationMapper.toResponse(entity);
    }

    // ==================== Creation Operations ====================

    @Override
    @Transactional
    public SystemNotificationResponse create(CreateSystemNotificationRequest request,
                                              Long createdByUserId, String createdByName) {
        // 1. Lưu master copy vào master DB
        SystemNotificationEntity entity = systemNotificationMapper.toEntity(request, createdByUserId, createdByName);
        SystemNotificationEntity savedEntity = systemNotificationRepository.save(entity);

        log.info("Đã lưu system notification master copy: id={}, targetAudience={}",
                savedEntity.getId(), savedEntity.getTargetAudience());

        // 2. Gửi notification cross-tenant (async-like, không ảnh hưởng response)
        sendNotificationToAllTenants(savedEntity);

        return systemNotificationMapper.toResponse(savedEntity);
    }

    // ==================== Cross-Tenant Operations ====================

    /**
     * Gửi notification đến tất cả tenant dựa trên target_audience.
     * Mỗi tenant được xử lý độc lập, lỗi 1 tenant không ảnh hưởng tenant khác.
     */
    private void sendNotificationToAllTenants(SystemNotificationEntity systemNotification) {
        Map<String, DataSource> allDataSources = tenantDataSourceManager.getAllDataSources();

        if (allDataSources.isEmpty()) {
            log.warn("Không có tenant nào để gửi system notification");
            return;
        }

        log.info("Bắt đầu gửi system notification {} đến {} tenant(s)",
                systemNotification.getId(), allDataSources.size());

        for (Map.Entry<String, DataSource> entry : allDataSources.entrySet()) {
            String tenantDomain = entry.getKey();
            DataSource dataSource = entry.getValue();

            try {
                sendNotificationToTenant(tenantDomain, dataSource, systemNotification);
            } catch (Exception e) {
                log.error("Lỗi khi gửi system notification đến tenant {}: {}",
                        tenantDomain, e.getMessage(), e);
            }
        }

        log.info("Hoàn thành gửi system notification {} đến tất cả tenant", systemNotification.getId());
    }

    /**
     * Gửi notification đến một tenant cụ thể.
     * Query users theo target_audience, chọn title/content theo region, insert notification bằng JDBC.
     */
    private void sendNotificationToTenant(String tenantDomain, DataSource dataSource,
                                           SystemNotificationEntity systemNotification) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Query users theo target_audience
        String selectSql = buildUserQuerySql(systemNotification.getTargetAudience());
        List<Map<String, Object>> users = jdbcTemplate.queryForList(selectSql);

        if (users.isEmpty()) {
            log.debug("Không có user phù hợp trong tenant {} cho target_audience {}",
                    tenantDomain, systemNotification.getTargetAudience());
            return;
        }

        log.info("Tìm thấy {} user(s) trong tenant {} cho target_audience {}",
                users.size(), tenantDomain, systemNotification.getTargetAudience());

        // Insert notification cho từng user
        for (Map<String, Object> user : users) {
            try {
                Long userId = ((Number) user.get("id")).longValue();
                String language = (String) user.get("language");

                // Chọn title/content theo region user
                String title = getTitleByLocale(systemNotification, language);
                String content = getContentByLocale(systemNotification, language);

                // Insert notification bằng JDBC với title, content, system_notification_id
                Long notificationId = insertSystemNotificationWithJdbc(
                        jdbcTemplate, userId, title, content, systemNotification.getId());

                if (notificationId != null) {
                    // Push WebSocket real-time
                    String targetUrl = "/me/notifications/" + notificationId;
                    NotificationResponse response = NotificationResponse.builder()
                            .id(notificationId)
                            .code(NotificationCode.SYSTEM_ANNOUNCEMENT)
                            .targetUrl(targetUrl)
                            .type(NotificationType.SYSTEM)
                            .isRead(false)
                            .title(title)
                            .content(content)
                            .systemNotificationId(systemNotification.getId())
                            .build();
                    notificationService.pushNotification(userId, response);
                }
            } catch (Exception e) {
                Long userId = user.get("id") != null ? ((Number) user.get("id")).longValue() : null;
                log.error("Lỗi khi gửi notification cho user {} trong tenant {}: {}",
                        userId, tenantDomain, e.getMessage());
            }
        }
    }

    /**
     * Build SQL query để lấy users theo target_audience.
     */
    private String buildUserQuerySql(TargetAudience targetAudience) {
        return switch (targetAudience) {
            case COMPANY_ADMINS -> """
                SELECT id, language FROM users 
                WHERE role = 'ADMIN_COMPANY' AND deleted = false
                """;
            case ALL_USERS -> """
                SELECT id, language FROM users 
                WHERE deleted = false AND status = 'ACTIVE'
                """;
        };
    }

    /**
     * Insert notification bằng JDBC với title, content, system_notification_id.
     * Pattern tương tự insertNotificationWithJdbc trong NotificationServiceImpl.
     *
     * @return notification ID hoặc null nếu lỗi
     */
    private Long insertSystemNotificationWithJdbc(JdbcTemplate jdbcTemplate, Long userId,
                                                   String title, String content,
                                                   Long systemNotificationId) {
        try {
            String insertSql = """
                INSERT INTO notifications (user_id, code, target_url, type, is_read, title, content, system_notification_id, created_at)
                VALUES (?, ?, ?, ?, false, ?, ?, ?, NOW())
                RETURNING id
                """;

            // Dùng placeholder cho target_url, sẽ update sau khi có notification ID
            Long notificationId = jdbcTemplate.queryForObject(
                    insertSql, Long.class,
                    userId,
                    NotificationCode.SYSTEM_ANNOUNCEMENT,
                    "", // placeholder target_url
                    NotificationType.SYSTEM.name(),
                    title,
                    content,
                    systemNotificationId);

            // Update target_url với notification ID thực tế
            if (notificationId != null) {
                String targetUrl = "/me/notifications/" + notificationId;
                jdbcTemplate.update(
                        "UPDATE notifications SET target_url = ? WHERE id = ?",
                        targetUrl, notificationId);
            }

            log.debug("Đã insert system notification cho user {}: notificationId={}", userId, notificationId);
            return notificationId;
        } catch (Exception e) {
            log.error("Lỗi khi insert system notification cho user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Lấy title theo region của user.
     * Fallback về tiếng Việt nếu region không xác định.
     */
    private String getTitleByLocale(SystemNotificationEntity entity, String language) {
        if (language == null) {
            return entity.getTitleVi();
        }
        return switch (language.toLowerCase()) {
            case "en" -> entity.getTitleEn();
            case "ja" -> entity.getTitleJa();
            default -> entity.getTitleVi();
        };
    }

    /**
     * Lấy content theo region của user.
     * Fallback về tiếng Việt nếu region không xác định.
     */
    private String getContentByLocale(SystemNotificationEntity entity, String language) {
        if (language == null) {
            return entity.getContentVi();
        }
        return switch (language.toLowerCase()) {
            case "en" -> entity.getContentEn();
            case "ja" -> entity.getContentJa();
            default -> entity.getContentVi();
        };
    }
}
