package com.tamabee.api_hr.service.core.interfaces;

import java.util.List;

import com.tamabee.api_hr.dto.request.attendance.ApplyPreferenceRequest;
import com.tamabee.api_hr.dto.request.attendance.ShiftPreferenceRequest;
import com.tamabee.api_hr.dto.response.attendance.ShiftPreferenceResponse;

/**
 * Service interface quản lý nguyện vọng ca làm việc.
 */
public interface IShiftPreferenceService {

    /**
     * Tạo nguyện vọng ca làm việc mới.
     * Validate: shift template tồn tại và active, tuần chưa qua, không trùng nguyện vọng.
     * Logic priority: có reason → HIGH, không → NORMAL.
     *
     * @param employeeId ID nhân viên
     * @param request    thông tin nguyện vọng
     * @return danh sách nguyện vọng đã tạo (1 per dayOfWeek)
     */
    List<ShiftPreferenceResponse> createPreference(Long employeeId, ShiftPreferenceRequest request);

    /**
     * Cập nhật nguyện vọng (chỉ khi status = PENDING).
     *
     * @param id      ID nguyện vọng
     * @param request thông tin cập nhật
     * @return nguyện vọng đã cập nhật
     */
    ShiftPreferenceResponse updatePreference(Long id, ShiftPreferenceRequest request);

    /**
     * Xóa nguyện vọng (chỉ khi status = PENDING).
     *
     * @param id ID nguyện vọng
     */
    void deletePreference(Long id);

    /**
     * Lấy tất cả nguyện vọng theo tuần (cho Manager).
     *
     * @param year       năm
     * @param weekNumber số tuần
     * @return danh sách nguyện vọng
     */
    List<ShiftPreferenceResponse> getPreferencesByWeek(Integer year, Integer weekNumber);

    /**
     * Lấy nguyện vọng của employee theo tuần.
     *
     * @param employeeId ID nhân viên
     * @param year       năm
     * @param weekNumber số tuần
     * @return danh sách nguyện vọng
     */
    List<ShiftPreferenceResponse> getMyPreferences(Long employeeId, Integer year, Integer weekNumber);

    /**
     * Áp dụng nguyện vọng thành shift assignment.
     * Tạo ShiftAssignment, cập nhật status → APPLIED.
     *
     * @param id           ID nguyện vọng
     * @param applyRequest thông tin áp dụng
     * @return nguyện vọng đã cập nhật
     */
    ShiftPreferenceResponse applyPreference(Long id, ApplyPreferenceRequest applyRequest);

    /**
     * Lấy tất cả nguyện vọng theo năm và danh sách tuần (cho month mode).
     *
     * @param year        năm
     * @param weekNumbers danh sách số tuần
     * @return danh sách nguyện vọng
     */
    List<ShiftPreferenceResponse> getPreferencesByWeeks(Integer year, List<Integer> weekNumbers);

    /**
     * Hoàn tác áp dụng nguyện vọng: xóa assignment, đặt lại status PENDING.
     *
     * @param id ID nguyện vọng
     */
    void revertPreference(Long id);
}
