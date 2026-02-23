package com.tamabee.api_hr.service.company.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.entity.company.AttendanceSettingEntity;
import com.tamabee.api_hr.entity.company.BreakSettingEntity;
import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import com.tamabee.api_hr.entity.company.OvertimeSettingEntity;
import com.tamabee.api_hr.entity.company.PayrollSettingEntity;
import com.tamabee.api_hr.mapper.company.AttendanceSettingMapper;
import com.tamabee.api_hr.mapper.company.BreakSettingMapper;
import com.tamabee.api_hr.mapper.company.CompanySettingMapper;
import com.tamabee.api_hr.mapper.company.OvertimeSettingMapper;
import com.tamabee.api_hr.mapper.company.PayrollSettingMapper;
import com.tamabee.api_hr.repository.company.AttendanceSettingRepository;
import com.tamabee.api_hr.repository.company.BreakSettingRepository;
import com.tamabee.api_hr.repository.company.CompanySettingsRepository;
import com.tamabee.api_hr.repository.company.OvertimeSettingRepository;
import com.tamabee.api_hr.repository.company.PayrollSettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper bean để tạo default settings trong transaction riêng (REQUIRES_NEW).
 * Giải quyết vấn đề INSERT trong read-only transaction khi auto-create default settings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSettingsInitializer {

    private final AttendanceSettingRepository attendanceSettingRepository;
    private final BreakSettingRepository breakSettingRepository;
    private final PayrollSettingRepository payrollSettingRepository;
    private final OvertimeSettingRepository overtimeSettingRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final AttendanceSettingMapper attendanceSettingMapper;
    private final BreakSettingMapper breakSettingMapper;
    private final PayrollSettingMapper payrollSettingMapper;
    private final OvertimeSettingMapper overtimeSettingMapper;
    private final CompanySettingMapper companySettingMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttendanceSettingEntity createDefaultAttendanceSetting() {
        log.info("Tạo default attendance setting cho tenant (REQUIRES_NEW)");
        AttendanceSettingEntity entity = attendanceSettingMapper.toEntity();
        return attendanceSettingRepository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BreakSettingEntity createDefaultBreakSetting() {
        log.info("Tạo default break setting cho tenant (REQUIRES_NEW)");
        BreakSettingEntity entity = breakSettingMapper.toEntity();
        return breakSettingRepository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PayrollSettingEntity createDefaultPayrollSetting() {
        log.info("Tạo default payroll setting cho tenant (REQUIRES_NEW)");
        PayrollSettingEntity entity = payrollSettingMapper.toEntity();
        return payrollSettingRepository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OvertimeSettingEntity createDefaultOvertimeSetting() {
        log.info("Tạo default overtime setting cho tenant (REQUIRES_NEW)");
        OvertimeSettingEntity entity = overtimeSettingMapper.toEntity();
        return overtimeSettingRepository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompanySettingEntity createDefaultCompanySettings() {
        log.info("Tạo default company settings cho tenant (REQUIRES_NEW)");
        CompanySettingEntity entity = companySettingMapper.toEntity();
        return companySettingsRepository.save(entity);
    }
}
