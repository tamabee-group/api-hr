package com.tamabee.api_hr.dto.response.attendance;

import java.time.LocalTime;

import lombok.Data;

/**
 * DTO cho khoảng thời gian chưa được bao phủ bởi shift template.
 */
@Data
public class TimeGap {

    private LocalTime start;
    private LocalTime end;
}
