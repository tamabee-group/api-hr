package com.tamabee.api_hr.service.calculator.interfaces;

import com.tamabee.api_hr.dto.config.OvertimeConfig;
import com.tamabee.api_hr.dto.config.OvertimeMultipliers;
import com.tamabee.api_hr.dto.result.DailyOvertimeDetail;
import com.tamabee.api_hr.dto.result.OvertimeResult;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface cho việc tính toán tăng ca
 */
public interface IOvertimeCalculator {

        /**
         * Tính toán tổng hợp tăng ca từ danh sách chi tiết theo ngày
         *
         * @param dailyDetails Danh sách chi tiết tăng ca theo ngày
         * @param config       Cấu hình tăng ca của công ty
         * @param hourlyRate   Lương theo giờ của nhân viên
         * @return Kết quả tính toán tăng ca
         */
        OvertimeResult calculateOvertime(
                        List<DailyOvertimeDetail> dailyDetails,
                        OvertimeConfig config,
                        BigDecimal hourlyRate);

        /**
         * Tính số phút tăng ca đêm từ khoảng thời gian làm việc
         *
         * @param totalOvertimeMinutes Tổng số phút tăng ca trong ngày
         * @param checkInHour          Giờ check-in
         * @param checkOutHour         Giờ check-out
         * @param config               Cấu hình tăng ca
         * @return Số phút tăng ca đêm
         */
        int calculateNightOvertimeMinutes(
                        int totalOvertimeMinutes,
                        int checkInHour,
                        int checkOutHour,
                        OvertimeConfig config);

        /**
         * Tính số phút làm trong giờ đêm (sau khi trừ break)
         *
         * @param checkIn      Thời gian check-in
         * @param checkOut     Thời gian check-out
         * @param breakRecords Danh sách bản ghi giải lao
         * @param config       Cấu hình tăng ca
         * @return Số phút làm trong giờ đêm
         */
        int calculateNightMinutes(
                        LocalDateTime checkIn,
                        LocalDateTime checkOut,
                        List<BreakRecordEntity> breakRecords,
                        OvertimeConfig config);

        /**
         * Lấy legal minimum multipliers theo locale
         *
         * @param locale Locale code (ja, vi, en, ...)
         * @return Hệ số tăng ca tối thiểu
         */
        OvertimeMultipliers getLegalMinimumMultipliers(String locale);

        /**
         * Validate custom multipliers không thấp hơn legal minimum
         *
         * @param config Cấu hình tăng ca
         * @return true nếu tất cả multipliers đều compliant
         */
        boolean validateMultipliers(OvertimeConfig config);
}
