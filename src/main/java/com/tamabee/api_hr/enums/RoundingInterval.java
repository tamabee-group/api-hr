package com.tamabee.api_hr.enums;

import lombok.Getter;

/**
 * Khoảng thời gian làm tròn (phút)
 */
@Getter
public enum RoundingInterval {
    MINUTES_5(5),
    MINUTES_10(10),
    MINUTES_15(15),
    MINUTES_30(30),
    MINUTES_60(60);

    private final int minutes;

    RoundingInterval(int minutes) {
        this.minutes = minutes;
    }
}
