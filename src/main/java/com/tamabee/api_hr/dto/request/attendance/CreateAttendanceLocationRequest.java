package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request tạo vị trí chấm công mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttendanceLocationRequest {

    /**
     * Tên vị trí
     */
    @NotBlank(message = "Tên vị trí không được để trống")
    @Size(max = 200, message = "Tên vị trí không được vượt quá 200 ký tự")
    private String name;

    /**
     * Địa chỉ
     */
    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    /**
     * Vĩ độ (từ -90 đến 90)
     */
    @NotNull(message = "Vĩ độ không được để trống")
    private Double latitude;

    /**
     * Kinh độ (từ -180 đến 180)
     */
    @NotNull(message = "Kinh độ không được để trống")
    private Double longitude;

    /**
     * Bán kính cho phép chấm công (mét), phải lớn hơn 0
     */
    @NotNull(message = "Bán kính không được để trống")
    private Integer radiusMeters;

    /**
     * Trạng thái hoạt động (mặc định: true)
     */
    @Builder.Default
    private Boolean isActive = true;
}
