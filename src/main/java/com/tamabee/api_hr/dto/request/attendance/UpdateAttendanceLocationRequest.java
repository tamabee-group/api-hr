package com.tamabee.api_hr.dto.request.attendance;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật vị trí chấm công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttendanceLocationRequest {

    /**
     * Tên vị trí
     */
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
    private Double latitude;

    /**
     * Kinh độ (từ -180 đến 180)
     */
    private Double longitude;

    /**
     * Bán kính cho phép chấm công (mét), phải lớn hơn 0
     */
    private Integer radiusMeters;

    /**
     * Trạng thái hoạt động
     */
    private Boolean isActive;
}
