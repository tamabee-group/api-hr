---
name: Backend Notifications
description: Notification system - sending notifications (single, bulk, Tamabee staff), notification codes, types, frontend integration
---

# Notifications System

## Backend - Gửi Notification

### Service Interface

```java
// Inject INotificationService
private final INotificationService notificationService;
```

### Gửi notification cho 1 user

```java
Map<String, Object> params = new HashMap<>();
params.put("employeeName", "Nguyễn Văn A");
params.put("startDate", "2026-01-15");

notificationService.createNotification(
    userId,                          // Long - ID của user nhận
    NotificationCode.LEAVE_APPROVED, // String - Code từ NotificationCode.java
    params,                          // Map<String, Object> - Params cho message template
    "/me/leave?id=" + leaveId,       // String - URL khi click vào notification
    NotificationType.LEAVE           // Enum - Type để group notifications
);
```

### Gửi notification cho nhiều users

```java
List<Long> userIds = List.of(1L, 2L, 3L);

notificationService.createBulkNotifications(
    userIds,
    NotificationCode.PAYROLL_CONFIRMED,
    params,
    "/me/payroll",
    NotificationType.PAYROLL
);
```

### Gửi notification cho Tamabee Admin/Manager

Dùng khi cần thông báo cho staff Tamabee (deposit mới, system alerts...):

```java
Map<String, Object> params = new HashMap<>();
params.put("companyName", "ABC Corp");
params.put("amount", "50000");

notificationService.notifyTamabeeStaff(
    NotificationCode.DEPOSIT_SUBMITTED,
    params,
    "/admin/deposits?id=" + depositId,
    NotificationType.WALLET
);
```

**Lưu ý**: Method này query trực tiếp từ tenant "tamabee" database, không phụ thuộc vào TenantContext hiện tại.

## Notification Codes

Định nghĩa trong `NotificationCode.java`:

| Code                 | Type       | Mô tả                                       |
| -------------------- | ---------- | ------------------------------------------- |
| WELCOME_COMPANY      | WELCOME    | Chào mừng công ty mới                       |
| WELCOME_EMPLOYEE     | WELCOME    | Chào mừng nhân viên mới                     |
| LEAVE_SUBMITTED      | LEAVE      | Có đơn nghỉ phép mới (gửi admin/manager)    |
| LEAVE_APPROVED       | LEAVE      | Đơn nghỉ phép được duyệt                    |
| LEAVE_REJECTED       | LEAVE      | Đơn nghỉ phép bị từ chối                    |
| ADJUSTMENT_SUBMITTED | ADJUSTMENT | Có yêu cầu điều chỉnh mới                   |
| ADJUSTMENT_APPROVED  | ADJUSTMENT | Yêu cầu điều chỉnh được duyệt               |
| ADJUSTMENT_REJECTED  | ADJUSTMENT | Yêu cầu điều chỉnh bị từ chối               |
| DEPOSIT_SUBMITTED    | WALLET     | Có yêu cầu nạp tiền mới (gửi Tamabee staff) |
| DEPOSIT_APPROVED     | WALLET     | Yêu cầu nạp tiền được duyệt                 |
| DEPOSIT_REJECTED     | WALLET     | Yêu cầu nạp tiền bị từ chối                 |
| PAYROLL_CONFIRMED    | PAYROLL    | Phiếu lương đã xác nhận                     |
| PAYROLL_PAID         | PAYROLL    | Lương đã thanh toán                         |
| SYSTEM_ANNOUNCEMENT  | SYSTEM     | Thông báo hệ thống từ Tamabee               |
| FEEDBACK_SUBMITTED   | FEEDBACK   | Có feedback mới (gửi Tamabee staff)         |
| FEEDBACK_REPLIED     | FEEDBACK   | Feedback đã được phản hồi                   |

## Notification Types

```java
public enum NotificationType {
    WELCOME,    // Chào mừng
    PAYROLL,    // Lương
    WALLET,     // Ví/Nạp tiền
    LEAVE,      // Nghỉ phép
    ADJUSTMENT, // Điều chỉnh chấm công
    SYSTEM,     // Hệ thống
    FEEDBACK    // Feedback/Góp ý
}
```

## Frontend - Nhận Notification Real-time

### Subscribe để auto-refresh data khi có notification

```typescript
import { subscribeToNotificationEvents } from "@/hooks/use-notifications";

useEffect(() => {
  const unsubscribe = subscribeToNotificationEvents("LEAVE", () => {
    fetchData();
  });
  return unsubscribe;
}, [fetchData]);
```

### Hook useNotifications

```typescript
const {
  notifications, // Notification[] - 5 notifications mới nhất
  unreadCount, // number - Số chưa đọc
  isLoading, // boolean
  isConnected, // boolean - WebSocket connected
  markAsRead, // (id: number) => Promise<void>
  markAllAsRead, // () => Promise<void>
  refetch, // () => Promise<void>
} = useNotifications();
```

## Frontend - Translation

Thêm message template trong `messages/{locale}/notifications.json` (locale = ngôn ngữ giao diện vi/en/ja, không phải region):

```json
{
  "codes": {
    "LEAVE_APPROVED": "Đơn xin nghỉ phép từ {startDate} đến {endDate} đã được duyệt",
    "DEPOSIT_SUBMITTED": "{companyName} đã gửi yêu cầu nạp tiền {amount}"
  }
}
```

Params trong backend sẽ được interpolate vào message template.
