package com.tamabee.api_hr.dto.request.leave;

import com.tamabee.api_hr.enums.HolidayType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request cập nhật ngày nghỉ lễ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHolidayRequest {

    /**
     * Ngày nghỉ
     */
    private LocalDate date;

    /**
     * Tên ngày nghỉ
     */
    @Size(max = 255, message = "Tên ngày nghỉ không được vượt quá 255 ký tự")
    private String name;

    /**
     * Loại ngày nghỉ: NATIONAL (quốc gia), COMPANY (công ty)
     */
    private HolidayType type;

    /**
     * Có được trả lương không
     */
    private Boolean isPaid;

    /**
     * Mô tả thêm
     */
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
}
