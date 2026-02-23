package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho danh sách nhân viên kèm trạng thái chấm công hôm nay.
 * Dùng cho màn hình kiosk hiển thị sidebar nhân viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskEmployeeStatusResponse {

    private Long employeeId;
    private String employeeCode;
    private String name;
    private String avatar;
    private String departmentName;

    // Trạng thái chấm công hôm nay
    private String status; // ONLINE, OFFLINE, BREAK, NOT_CHECKED_IN
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    // Danh sách break records
    private List<BreakPeriod> breaks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakPeriod {
        private LocalDateTime start;
        private LocalDateTime end;
    }
}
