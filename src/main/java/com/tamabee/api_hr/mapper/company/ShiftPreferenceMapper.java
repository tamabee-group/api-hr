package com.tamabee.api_hr.mapper.company;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.dto.request.attendance.ShiftPreferenceRequest;
import com.tamabee.api_hr.dto.response.attendance.EmployeePreferenceSummary;
import com.tamabee.api_hr.dto.response.attendance.ShiftPreferenceResponse;
import com.tamabee.api_hr.entity.attendance.ShiftPreferenceEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.PreferencePriority;
import com.tamabee.api_hr.enums.PreferenceStatus;

/**
 * Mapper chuyển đổi giữa ShiftPreference entities và DTOs.
 */
@Component
public class ShiftPreferenceMapper {

    private final ShiftMapper shiftMapper;

    public ShiftPreferenceMapper(ShiftMapper shiftMapper) {
        this.shiftMapper = shiftMapper;
    }

    /**
     * Chuyển đổi ShiftPreferenceEntity sang response.
     *
     * @param entity        entity cần chuyển đổi
     * @param employee      entity nhân viên (để lấy tên)
     * @param shiftTemplate entity mẫu ca (nullable nếu custom time)
     * @return response
     */
    public ShiftPreferenceResponse toResponse(
            ShiftPreferenceEntity entity,
            UserEntity employee,
            ShiftTemplateEntity shiftTemplate) {
        if (entity == null) {
            return null;
        }

        ShiftPreferenceResponse response = new ShiftPreferenceResponse();
        response.setId(entity.getId());
        response.setEmployeeId(entity.getEmployeeId());
        response.setEmployeeName(
                employee != null && employee.getProfile() != null ? employee.getProfile().getName() : null);
        response.setYear(entity.getYear());
        response.setWeekNumber(entity.getWeekNumber());
        response.setDayOfWeek(entity.getDayOfWeek());
        response.setShiftTemplate(shiftMapper.toResponse(shiftTemplate));
        response.setCustomStartTime(entity.getCustomStartTime());
        response.setCustomEndTime(entity.getCustomEndTime());
        response.setReason(entity.getReason());
        response.setPriority(entity.getPriority());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }

    /**
     * Chuyển đổi request sang entity cho một ngày cụ thể.
     * Tạo một entity cho mỗi dayOfWeek trong request.
     *
     * @param request    request tạo nguyện vọng
     * @param employeeId ID nhân viên
     * @param dayOfWeek  ngày trong tuần (1=Monday..7=Sunday)
     * @return entity
     */
    public ShiftPreferenceEntity toEntity(ShiftPreferenceRequest request, Long employeeId, Integer dayOfWeek) {
        if (request == null) {
            return null;
        }

        ShiftPreferenceEntity entity = new ShiftPreferenceEntity();
        entity.setEmployeeId(employeeId);
        entity.setYear(request.getYear());
        entity.setWeekNumber(request.getWeekNumber());
        entity.setDayOfWeek(dayOfWeek);
        entity.setShiftTemplateId(request.getShiftTemplateId());
        entity.setCustomStartTime(request.getCustomStartTime());
        entity.setCustomEndTime(request.getCustomEndTime());
        entity.setReason(request.getReason());
        entity.setPriority(request.getReason() != null && !request.getReason().isBlank()
                ? PreferencePriority.HIGH
                : PreferencePriority.NORMAL);
        entity.setStatus(PreferenceStatus.PENDING);

        return entity;
    }

    /**
     * Chuyển đổi entity sang EmployeePreferenceSummary (cho suggestion engine).
     *
     * @param entity   entity nguyện vọng
     * @param employee entity nhân viên (để lấy tên)
     * @return summary
     */
    public EmployeePreferenceSummary toSummary(ShiftPreferenceEntity entity, UserEntity employee) {
        if (entity == null) {
            return null;
        }

        EmployeePreferenceSummary summary = new EmployeePreferenceSummary();
        summary.setEmployeeId(entity.getEmployeeId());
        summary.setEmployeeName(
                employee != null && employee.getProfile() != null ? employee.getProfile().getName() : null);
        summary.setReason(entity.getReason());
        summary.setPriority(entity.getPriority());

        return summary;
    }
}
