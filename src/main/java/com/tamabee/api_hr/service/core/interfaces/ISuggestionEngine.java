package com.tamabee.api_hr.service.core.interfaces;

import java.time.LocalTime;
import java.util.List;

import com.tamabee.api_hr.dto.response.attendance.CustomTimeResolution;
import com.tamabee.api_hr.dto.response.attendance.SuggestionResponse;

/**
 * Engine gợi ý phân ca dựa trên nguyện vọng + dữ liệu lịch sử.
 * Xét Company_Setting holidays và weekends khi đưa ra gợi ý.
 */
public interface ISuggestionEngine {

    /**
     * Gợi ý phân ca cho một tuần dựa trên nguyện vọng hiện tại và dữ liệu lịch sử.
     * - Kết hợp nguyện vọng + lịch sử + holidays
     * - Sắp xếp nhân viên theo priority (HIGH trước NORMAL)
     * - Nhân viên không có nguyện vọng xuất hiện trong flexibleEmployees
     * - Loại trừ ngày nghỉ lễ và cuối tuần theo Company_Setting
     *
     * @param year       năm
     * @param weekNumber số tuần (ISO 8601)
     * @return danh sách gợi ý theo từng ngày trong tuần
     */
    List<SuggestionResponse> getSuggestions(Integer year, Integer weekNumber);

    /**
     * Tìm shift templates phù hợp cho custom time.
     * - Tìm tất cả template có thời gian giao nhau với custom time
     * - Tính coverage percentage cho mỗi template
     * - Xác định gaps (khoảng thời gian chưa được bao phủ)
     * - fullyCovered = true nếu tổng templates bao phủ hoàn toàn custom time
     *
     * @param customStart giờ bắt đầu custom
     * @param customEnd   giờ kết thúc custom
     * @return kết quả phân giải custom time
     */
    CustomTimeResolution findMatchingTemplates(LocalTime customStart, LocalTime customEnd);

    /**
     * Phân tích mô hình lặp lại theo thứ trong tuần.
     * - Nhóm dữ liệu lịch sử theo dayOfWeek (1-7), không theo ngày cụ thể
     * - Tính trung bình số nhân viên cho mỗi shift template vào thứ đó
     *
     * @param dayOfWeek ngày trong tuần (1=Monday..7=Sunday)
     * @param weeksBack số tuần lịch sử cần phân tích
     * @return số lượng trung bình nhân viên theo mô hình lặp lại
     */
    Integer analyzeWeekdayPatterns(Integer dayOfWeek, Integer weeksBack);

    /**
     * Sao chép lịch phân ca từ tuần nguồn sang tuần đích.
     * - Ánh xạ theo thứ trong tuần (thứ 2 nguồn → thứ 2 đích, ...)
     * - Bảo toàn dayOfWeek mapping
     *
     * @param sourceYear năm nguồn
     * @param sourceWeek tuần nguồn
     * @param targetYear năm đích
     * @param targetWeek tuần đích
     */
    void copyScheduleFromPeriod(Integer sourceYear, Integer sourceWeek, Integer targetYear, Integer targetWeek);
}
