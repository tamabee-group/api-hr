package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response chứa thông tin vị trí chấm công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLocationResponse {

    private Long id;

    // Tên vị trí
    private String name;

    // Địa chỉ
    private String address;

    // Vĩ độ
    private Double latitude;

    // Kinh độ
    private Double longitude;

    // Bán kính cho phép chấm công (mét)
    private Integer radiusMeters;

    // Trạng thái hoạt động
    private Boolean isActive;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
