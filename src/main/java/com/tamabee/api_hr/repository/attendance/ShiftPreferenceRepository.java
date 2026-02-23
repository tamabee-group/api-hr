package com.tamabee.api_hr.repository.attendance;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.attendance.ShiftPreferenceEntity;

/**
 * Repository quản lý nguyện vọng ca làm việc.
 */
@Repository
public interface ShiftPreferenceRepository extends JpaRepository<ShiftPreferenceEntity, Long> {

    /**
     * Lấy danh sách nguyện vọng của nhân viên theo tuần
     */
    List<ShiftPreferenceEntity> findByEmployeeIdAndYearAndWeekNumber(Long employeeId, Integer year, Integer weekNumber);

    /**
     * Lấy tất cả nguyện vọng theo tuần
     */
    List<ShiftPreferenceEntity> findByYearAndWeekNumber(Integer year, Integer weekNumber);

    /**
     * Lấy tất cả nguyện vọng theo năm và danh sách tuần (dùng cho month mode)
     */
    List<ShiftPreferenceEntity> findByYearAndWeekNumberIn(Integer year, List<Integer> weekNumbers);

    /**
     * Kiểm tra nguyện vọng trùng lặp (cùng employee, tuần, ngày, ca template)
     */
    Optional<ShiftPreferenceEntity> findByEmployeeIdAndYearAndWeekNumberAndDayOfWeekAndShiftTemplateId(
            Long employeeId, Integer year, Integer weekNumber, Integer dayOfWeek, Long shiftTemplateId);

    /**
     * Kiểm tra nguyện vọng trùng lặp (cùng employee, tuần, ngày, custom time)
     */
    Optional<ShiftPreferenceEntity> findByEmployeeIdAndYearAndWeekNumberAndDayOfWeekAndCustomStartTimeAndCustomEndTime(
            Long employeeId, Integer year, Integer weekNumber, Integer dayOfWeek,
            LocalTime customStartTime, LocalTime customEndTime);
}
