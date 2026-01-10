package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.request.StartBreakRequest;
import com.tamabee.api_hr.dto.response.BreakRecordResponse;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mapper chuyển đổi giữa BreakRecordEntity và DTOs
 */
@Component
public class BreakRecordMapper {

    /**
     * Tạo entity mới cho bắt đầu giải lao
     */
    public BreakRecordEntity toEntity(
            Long attendanceRecordId,
            Long employeeId,
            LocalDate workDate,
            LocalDateTime breakStart,
            StartBreakRequest request) {

        BreakRecordEntity entity = new BreakRecordEntity();
        entity.setAttendanceRecordId(attendanceRecordId);
        entity.setEmployeeId(employeeId);
        entity.setWorkDate(workDate);
        entity.setBreakStart(breakStart);

        if (request != null && request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        return entity;
    }

    /**
     * Chuyển entity sang response
     */
    public BreakRecordResponse toResponse(BreakRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        return BreakRecordResponse.builder()
                .id(entity.getId())
                .breakNumber(entity.getBreakNumber())
                .breakStart(entity.getBreakStart())
                .breakEnd(entity.getBreakEnd())
                .actualBreakMinutes(entity.getActualBreakMinutes())
                .effectiveBreakMinutes(entity.getEffectiveBreakMinutes())
                .notes(entity.getNotes())
                .isActive(entity.getBreakEnd() == null)
                .build();
    }

    /**
     * Cập nhật entity khi kết thúc giải lao
     */
    public void updateEndBreak(
            BreakRecordEntity entity,
            LocalDateTime breakEnd,
            Integer actualBreakMinutes,
            Integer effectiveBreakMinutes) {

        if (entity == null) {
            return;
        }

        entity.setBreakEnd(breakEnd);
        entity.setActualBreakMinutes(actualBreakMinutes);
        entity.setEffectiveBreakMinutes(effectiveBreakMinutes);
    }
}
