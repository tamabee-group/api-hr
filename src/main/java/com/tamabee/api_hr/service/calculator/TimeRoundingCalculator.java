package com.tamabee.api_hr.service.calculator;

import com.tamabee.api_hr.dto.config.RoundingConfig;
import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.RoundingInterval;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Calculator làm tròn thời gian chấm công
 * Hỗ trợ các interval: 5, 10, 15, 30, 60 phút
 * Hỗ trợ các direction: UP, DOWN, NEAREST
 */
@Component
public class TimeRoundingCalculator implements ITimeRoundingCalculator {

    @Override
    public LocalDateTime roundTime(LocalDateTime time, RoundingConfig config) {
        if (time == null || config == null) {
            return time;
        }

        RoundingInterval interval = config.getInterval();
        RoundingDirection direction = config.getDirection();

        if (interval == null || direction == null) {
            return time;
        }

        int intervalMinutes = interval.getMinutes();
        int minute = time.getMinute();

        // Tính số phút đã qua kể từ đầu giờ
        int remainder = minute % intervalMinutes;

        // Nếu đã ở đúng mốc làm tròn, không cần làm gì
        if (remainder == 0) {
            return time.truncatedTo(ChronoUnit.MINUTES);
        }

        LocalDateTime truncated = time.truncatedTo(ChronoUnit.MINUTES);

        return switch (direction) {
            case UP -> roundUp(truncated, intervalMinutes, remainder);
            case DOWN -> roundDown(truncated, remainder);
            case NEAREST -> roundNearest(truncated, intervalMinutes, remainder);
        };
    }

    /**
     * Làm tròn lên mốc interval tiếp theo
     */
    private LocalDateTime roundUp(LocalDateTime time, int intervalMinutes, int remainder) {
        int minutesToAdd = intervalMinutes - remainder;
        return time.plusMinutes(minutesToAdd);
    }

    /**
     * Làm tròn xuống mốc interval trước đó
     */
    private LocalDateTime roundDown(LocalDateTime time, int remainder) {
        return time.minusMinutes(remainder);
    }

    /**
     * Làm tròn đến mốc interval gần nhất
     */
    private LocalDateTime roundNearest(LocalDateTime time, int intervalMinutes, int remainder) {
        int halfInterval = intervalMinutes / 2;

        if (remainder < halfInterval) {
            // Gần mốc trước hơn -> làm tròn xuống
            return roundDown(time, remainder);
        } else if (remainder > halfInterval) {
            // Gần mốc sau hơn -> làm tròn lên
            return roundUp(time, intervalMinutes, remainder);
        } else {
            // Đúng giữa -> làm tròn lên (convention)
            return roundUp(time, intervalMinutes, remainder);
        }
    }
}
