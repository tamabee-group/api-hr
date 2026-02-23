package com.tamabee.api_hr.service.company.interfaces;

import java.util.List;

import com.tamabee.api_hr.dto.response.leave.HolidayResponse;

/**
 * Service tích hợp Google Calendar API để sync ngày lễ quốc gia.
 * Hỗ trợ lấy ngày lễ cho Việt Nam (vi) và Nhật Bản (ja).
 */
public interface IGoogleCalendarService {

    /**
     * Sync ngày lễ quốc gia từ Google Calendar API.
     * Tự động lấy region từ company trong master DB.
     *
     * @param year năm cần sync
     * @return danh sách ngày lễ đã sync
     */
    List<HolidayResponse> syncHolidays(Integer year);
}
