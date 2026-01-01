package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.HolidayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request tạo ngày nghỉ lễ mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHolidayRequest {

    /**
     * Ngày nghỉ
     */
    @NotNull(message = "Ngày nghỉ không được để trống")
    private LocalDate date;

    /**
     * Tên ngày nghỉ
     */
    @NotBlank(message = "Tên ngày nghỉ không được để trống")
    @Size(max = 255, message = "Tên ngày nghỉ không được vượt quá 255 ký tự")
    private String name;

    /**
     * Loại ngày nghỉ: NATIONAL (quốc gia), COMPANY (công ty)
     */
    @NotNull(message = "Loại ngày nghỉ không được để trống")
    private HolidayType type;

    /**
     * Có được trả lương không (mặc định: true)
     */
    private Boolean isPaid = true;

    /**
     * Mô tả thêm
     */
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
}
