package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.request.leave.CreateHolidayRequest;
import com.tamabee.api_hr.dto.request.leave.UpdateHolidayRequest;
import com.tamabee.api_hr.dto.response.leave.HolidayResponse;
import com.tamabee.api_hr.entity.leave.HolidayEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.HolidayMapper;
import com.tamabee.api_hr.repository.leave.HolidayRepository;
import com.tamabee.api_hr.service.company.interfaces.IHolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation quản lý ngày nghỉ lễ.
 * Hỗ trợ ngày lễ quốc gia và ngày nghỉ riêng của công ty.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements IHolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;

    // ==================== CRUD Operations ====================

    @Override
    @Transactional
    public HolidayResponse createHoliday(CreateHolidayRequest request) {
        // Kiểm tra ngày nghỉ đã tồn tại chưa
        if (holidayRepository.existsByDateAndDeletedFalse(request.getDate())) {
            throw new ConflictException(
                    "Ngày nghỉ lễ đã tồn tại cho ngày " + request.getDate(),
                    ErrorCode.HOLIDAY_DATE_EXISTS);
        }

        HolidayEntity entity = holidayMapper.toEntity(request);
        entity = holidayRepository.save(entity);

        log.info("Đã tạo ngày nghỉ lễ {} cho ngày {}", entity.getId(), request.getDate());
        return holidayMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public HolidayResponse updateHoliday(Long holidayId, UpdateHolidayRequest request) {
        HolidayEntity entity = findHoliday(holidayId);

        // Nếu thay đổi ngày, kiểm tra ngày mới đã tồn tại chưa
        if (request.getDate() != null && !request.getDate().equals(entity.getDate())) {
            if (holidayRepository.existsByDateAndDeletedFalse(request.getDate())) {
                throw new ConflictException(
                        "Ngày nghỉ lễ đã tồn tại cho ngày " + request.getDate(),
                        ErrorCode.HOLIDAY_DATE_EXISTS);
            }
        }

        holidayMapper.updateEntity(entity, request);
        entity = holidayRepository.save(entity);

        log.info("Đã cập nhật ngày nghỉ lễ {}", holidayId);
        return holidayMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteHoliday(Long holidayId) {
        HolidayEntity entity = findHoliday(holidayId);
        entity.setDeleted(true);
        holidayRepository.save(entity);

        log.info("Đã xóa ngày nghỉ lễ {}", holidayId);
    }

    // ==================== Query Operations ====================

    @Override
    @Transactional(readOnly = true)
    public HolidayResponse getHolidayById(Long holidayId) {
        HolidayEntity entity = findHoliday(holidayId);
        return holidayMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HolidayResponse> getHolidays(Integer year, Pageable pageable) {
        if (year != null) {
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            return holidayRepository
                    .findByDateBetweenAndDeletedFalse(startDate, endDate, pageable)
                    .map(holidayMapper::toResponse);
        }
        return holidayRepository.findByDeletedFalse(pageable)
                .map(holidayMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByDateBetween(startDate, endDate)
                .stream()
                .map(holidayMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> getNationalHolidays(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findNationalHolidaysByDateBetween(startDate, endDate)
                .stream()
                .map(holidayMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    /**
     * Tìm ngày nghỉ theo ID
     */
    private HolidayEntity findHoliday(Long holidayId) {
        return holidayRepository.findByIdAndDeletedFalse(holidayId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy ngày nghỉ lễ",
                        ErrorCode.HOLIDAY_NOT_FOUND));
    }
}
