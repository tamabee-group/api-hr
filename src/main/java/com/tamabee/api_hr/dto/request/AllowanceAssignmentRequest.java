package com.tamabee.api_hr.dto.request;

import com.tamabee.api_hr.enums.AllowanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO để gán phụ cấp cho nhân viên
 */
@Data
public class AllowanceAssignmentRequest {

    @NotBlank(message = "Mã phụ cấp không được để trống")
    @Size(max = 50, message = "Mã phụ cấp không được vượt quá 50 ký tự")
    private String allowanceCode;

    @NotBlank(message = "Tên phụ cấp không được để trống")
    @Size(max = 200, message = "Tên phụ cấp không được vượt quá 200 ký tự")
    private String allowanceName;

    @NotNull(message = "Loại phụ cấp không được để trống")
    private AllowanceType allowanceType;

    @NotNull(message = "Số tiền phụ cấp không được để trống")
    @Positive(message = "Số tiền phụ cấp phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Trạng thái tính thuế không được để trống")
    private Boolean taxable;

    @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
