package com.tamabee.api_hr.mapper.company;

import com.tamabee.api_hr.dto.config.BreakConfig;
import com.tamabee.api_hr.dto.request.BreakConfigRequest;
import com.tamabee.api_hr.dto.response.BreakConfigResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Mapper chuyển đổi giữa BreakConfig và DTOs
 */
@Component
public class BreakConfigMapper {

    /**
     * Chuyển BreakConfig sang response
     */
    public BreakConfigResponse toResponse(BreakConfig config) {
        if (config == null) {
            return null;
        }

        return BreakConfigResponse.builder()
                .breakEnabled(config.getBreakEnabled())
                .breakType(config.getBreakType())
                .defaultBreakMinutes(config.getDefaultBreakMinutes())
                .minimumBreakMinutes(config.getMinimumBreakMinutes())
                .maximumBreakMinutes(config.getMaximumBreakMinutes())
                .useLegalMinimum(config.getUseLegalMinimum())
                .breakTrackingEnabled(config.getBreakTrackingEnabled())
                .locale(config.getLocale())
                .fixedBreakMode(config.getFixedBreakMode())
                .breakPeriodsPerAttendance(config.getBreakPeriodsPerAttendance())
                .maxBreaksPerDay(config.getMaxBreaksPerDay())
                .fixedBreakPeriods(config.getFixedBreakPeriods())
                .nightShiftStartTime(config.getNightShiftStartTime())
                .nightShiftEndTime(config.getNightShiftEndTime())
                .nightShiftMinimumBreakMinutes(config.getNightShiftMinimumBreakMinutes())
                .nightShiftDefaultBreakMinutes(config.getNightShiftDefaultBreakMinutes())
                .build();
    }

    /**
     * Tạo BreakConfig mới từ request
     */
    public BreakConfig toConfig(BreakConfigRequest request) {
        if (request == null) {
            return BreakConfig.builder().build();
        }

        return BreakConfig.builder()
                .breakEnabled(request.getBreakEnabled() != null ? request.getBreakEnabled() : true)
                .breakType(request.getBreakType())
                .defaultBreakMinutes(request.getDefaultBreakMinutes() != null ? request.getDefaultBreakMinutes() : 60)
                .minimumBreakMinutes(request.getMinimumBreakMinutes() != null ? request.getMinimumBreakMinutes() : 45)
                .maximumBreakMinutes(request.getMaximumBreakMinutes() != null ? request.getMaximumBreakMinutes() : 90)
                .useLegalMinimum(request.getUseLegalMinimum() != null ? request.getUseLegalMinimum() : true)
                .breakTrackingEnabled(
                        request.getBreakTrackingEnabled() != null ? request.getBreakTrackingEnabled() : false)
                .locale(request.getLocale() != null ? request.getLocale() : "ja")
                .fixedBreakMode(request.getFixedBreakMode() != null ? request.getFixedBreakMode() : false)
                .breakPeriodsPerAttendance(
                        request.getBreakPeriodsPerAttendance() != null ? request.getBreakPeriodsPerAttendance() : 1)
                .maxBreaksPerDay(request.getMaxBreaksPerDay() != null ? request.getMaxBreaksPerDay() : 3)
                .fixedBreakPeriods(
                        request.getFixedBreakPeriods() != null ? request.getFixedBreakPeriods() : new ArrayList<>())
                .nightShiftStartTime(request.getNightShiftStartTime())
                .nightShiftEndTime(request.getNightShiftEndTime())
                .nightShiftMinimumBreakMinutes(
                        request.getNightShiftMinimumBreakMinutes() != null ? request.getNightShiftMinimumBreakMinutes()
                                : 45)
                .nightShiftDefaultBreakMinutes(
                        request.getNightShiftDefaultBreakMinutes() != null ? request.getNightShiftDefaultBreakMinutes()
                                : 60)
                .build();
    }

    /**
     * Cập nhật BreakConfig từ request (chỉ cập nhật các field không null)
     */
    public void updateConfig(BreakConfig config, BreakConfigRequest request) {
        if (config == null || request == null) {
            return;
        }

        if (request.getBreakEnabled() != null) {
            config.setBreakEnabled(request.getBreakEnabled());
        }
        if (request.getBreakType() != null) {
            config.setBreakType(request.getBreakType());
        }
        if (request.getDefaultBreakMinutes() != null) {
            config.setDefaultBreakMinutes(request.getDefaultBreakMinutes());
        }
        if (request.getMinimumBreakMinutes() != null) {
            config.setMinimumBreakMinutes(request.getMinimumBreakMinutes());
        }
        if (request.getMaximumBreakMinutes() != null) {
            config.setMaximumBreakMinutes(request.getMaximumBreakMinutes());
        }
        if (request.getUseLegalMinimum() != null) {
            config.setUseLegalMinimum(request.getUseLegalMinimum());
        }
        if (request.getBreakTrackingEnabled() != null) {
            config.setBreakTrackingEnabled(request.getBreakTrackingEnabled());
        }
        if (request.getLocale() != null) {
            config.setLocale(request.getLocale());
        }
        if (request.getFixedBreakMode() != null) {
            config.setFixedBreakMode(request.getFixedBreakMode());
        }
        if (request.getBreakPeriodsPerAttendance() != null) {
            config.setBreakPeriodsPerAttendance(request.getBreakPeriodsPerAttendance());
        }
        if (request.getMaxBreaksPerDay() != null) {
            config.setMaxBreaksPerDay(request.getMaxBreaksPerDay());
        }
        if (request.getFixedBreakPeriods() != null) {
            config.setFixedBreakPeriods(request.getFixedBreakPeriods());
        }
        if (request.getNightShiftStartTime() != null) {
            config.setNightShiftStartTime(request.getNightShiftStartTime());
        }
        if (request.getNightShiftEndTime() != null) {
            config.setNightShiftEndTime(request.getNightShiftEndTime());
        }
        if (request.getNightShiftMinimumBreakMinutes() != null) {
            config.setNightShiftMinimumBreakMinutes(request.getNightShiftMinimumBreakMinutes());
        }
        if (request.getNightShiftDefaultBreakMinutes() != null) {
            config.setNightShiftDefaultBreakMinutes(request.getNightShiftDefaultBreakMinutes());
        }
    }
}
