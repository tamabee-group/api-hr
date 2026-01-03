package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.DeductionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO để gán khấu trừ cho nhân viên
 */
@Data
public class DeductionAssignmentRequest {

    @NotBlank(message = "Mã khấu trừ không được để trống")
    @Size(max = 50, message = "Mã khấu trừ không được vượt quá 50 ký tự")
    private String deductionCode;

    @NotBlank(message = "Tên khấu trừ không được để trống")
    @Size(max = 200, message = "Tên khấu trừ không được vượt quá 200 ký tự")
    private String deductionName;

    @NotNull(message = "Loại khấu trừ không được để trống")
    private DeductionType deductionType;

    // Số tiền khấu trừ cố định (cho FIXED type)
    @PositiveOrZero(message = "Số tiền khấu trừ phải >= 0")
    private BigDecimal amount;

    // Phần trăm khấu trừ (cho PERCENTAGE type)
    @PositiveOrZero(message = "Phần trăm khấu trừ phải >= 0")
    private BigDecimal percentage;

    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
