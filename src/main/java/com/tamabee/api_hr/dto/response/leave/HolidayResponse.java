package com.tamabee.api_hr.dto.response.leave;

import com.tamabee.api_hr.enums.HolidayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response chứa thông tin ngày nghỉ lễ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {

    private Long id;

    // ID công ty (null = ngày lễ quốc gia)
    private Long companyId;

    // Ngày nghỉ
    private LocalDate date;

    // Tên ngày nghỉ
    private String name;

    // Loại ngày nghỉ
    private HolidayType type;

    // Có được trả lương không
    private Boolean isPaid;

    // Mô tả thêm
    private String description;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
