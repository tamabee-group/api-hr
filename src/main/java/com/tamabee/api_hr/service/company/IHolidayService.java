package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.CreateHolidayRequest;
import com.tamabee.api_hr.dto.request.UpdateHolidayRequest;
import com.tamabee.api_hr.dto.response.HolidayResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service quản lý ngày nghỉ lễ.
 * Hỗ trợ ngày lễ quốc gia và ngày nghỉ riêng của công ty.
 */
public interface IHolidayService {

    // ==================== CRUD Operations ====================

    /**
     * Tạo ngày nghỉ lễ mới
     *
     * @param request thông tin ngày nghỉ
     * @return ngày nghỉ đã tạo
     */
    HolidayResponse createHoliday(CreateHolidayRequest request);

    /**
     * Cập nhật ngày nghỉ lễ
     *
     * @param holidayId ID ngày nghỉ
     * @param request   thông tin cập nhật
     * @return ngày nghỉ đã cập nhật
     */
    HolidayResponse updateHoliday(Long holidayId, UpdateHolidayRequest request);

    /**
     * Xóa ngày nghỉ lễ (soft delete)
     *
     * @param holidayId ID ngày nghỉ
     */
    void deleteHoliday(Long holidayId);

    // ==================== Query Operations ====================

    /**
     * Lấy ngày nghỉ theo ID
     *
     * @param holidayId ID ngày nghỉ
     * @return ngày nghỉ
     */
    HolidayResponse getHolidayById(Long holidayId);

    /**
     * Lấy danh sách ngày nghỉ của công ty (phân trang, filter theo năm)
     *
     * @param year     năm cần filter (null = tất cả)
     * @param pageable thông tin phân trang
     * @return danh sách ngày nghỉ
     */
    Page<HolidayResponse> getHolidays(Integer year, Pageable pageable);

    /**
     * Lấy danh sách ngày nghỉ của công ty trong khoảng thời gian
     *
     * @param startDate ngày bắt đầu
     * @param endDate   ngày kết thúc
     * @return danh sách ngày nghỉ
     */
    List<HolidayResponse> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Lấy danh sách ngày lễ quốc gia trong khoảng thời gian
     *
     * @param startDate ngày bắt đầu
     * @param endDate   ngày kết thúc
     * @return danh sách ngày lễ quốc gia
     */
    List<HolidayResponse> getNationalHolidays(LocalDate startDate, LocalDate endDate);
}
