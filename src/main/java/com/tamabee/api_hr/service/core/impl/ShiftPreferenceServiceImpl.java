package com.tamabee.api_hr.service.core.impl;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.dto.request.attendance.ApplyPreferenceRequest;
import com.tamabee.api_hr.dto.request.attendance.ShiftPreferenceRequest;
import com.tamabee.api_hr.dto.response.attendance.ShiftPreferenceResponse;
import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.ShiftPreferenceEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ApplyMode;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.PreferencePriority;
import com.tamabee.api_hr.enums.PreferenceStatus;
import com.tamabee.api_hr.enums.ShiftAssignmentStatus;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.ShiftPreferenceMapper;
import com.tamabee.api_hr.repository.attendance.ShiftAssignmentRepository;
import com.tamabee.api_hr.repository.attendance.ShiftPreferenceRepository;
import com.tamabee.api_hr.repository.attendance.ShiftTemplateRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.core.interfaces.IShiftPreferenceService;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation quản lý nguyện vọng ca làm việc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftPreferenceServiceImpl implements IShiftPreferenceService {

    private final ShiftPreferenceRepository shiftPreferenceRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final UserRepository userRepository;
    private final ShiftPreferenceMapper shiftPreferenceMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /** WebSocket destination cho shift preference updates (gửi đến Manager) */
    private static final String PREFERENCE_UPDATE_DESTINATION = "/queue/shift-preferences";

    /** Các role Manager/Admin trong company */
    private static final List<UserRole> MANAGER_ROLES = List.of(
            UserRole.ADMIN_COMPANY, UserRole.MANAGER_COMPANY);

    @Override
    @Transactional
    public List<ShiftPreferenceResponse> createPreference(Long employeeId, ShiftPreferenceRequest request) {
        // Validate tuần chưa qua
        validateWeekNotPassed(request.getYear(), request.getWeekNumber());

        // Validate shift template nếu có
        ShiftTemplateEntity template = null;
        if (request.getShiftTemplateId() != null) {
            template = shiftTemplateRepository.findByIdAndDeletedFalse(request.getShiftTemplateId())
                    .filter(ShiftTemplateEntity::getIsActive)
                    .orElseThrow(() -> new NotFoundException(
                            "Mẫu ca không tồn tại hoặc đã bị vô hiệu",
                            ErrorCode.SHIFT_TEMPLATE_INACTIVE));
        }

        // Validate custom time nếu có
        if (request.getCustomStartTime() != null && request.getCustomEndTime() != null) {
            if (!request.getCustomStartTime().isBefore(request.getCustomEndTime())) {
                throw new BadRequestException(
                        "Giờ bắt đầu phải trước giờ kết thúc",
                        ErrorCode.SHIFT_PREFERENCE_INVALID_CUSTOM_TIME);
            }
        }

        // Xác định danh sách ngày
        List<Integer> daysOfWeek = request.getDaysOfWeek();
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            daysOfWeek = List.of(1, 2, 3, 4, 5, 6, 7);
        }

        // Validate dayOfWeek
        for (Integer day : daysOfWeek) {
            if (day < 1 || day > 7) {
                throw new BadRequestException(
                        "Ngày trong tuần không hợp lệ",
                        ErrorCode.SHIFT_PREFERENCE_INVALID_DAY);
            }
        }

        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId).orElse(null);
        List<ShiftPreferenceResponse> results = new ArrayList<>();

        for (Integer dayOfWeek : daysOfWeek) {
            // Kiểm tra trùng lặp
            checkDuplicate(employeeId, request, dayOfWeek);

            // Tạo entity qua mapper
            ShiftPreferenceEntity entity = shiftPreferenceMapper.toEntity(request, employeeId, dayOfWeek);
            ShiftPreferenceEntity saved = shiftPreferenceRepository.save(entity);

            results.add(shiftPreferenceMapper.toResponse(saved, employee, template));
        }

        // Push WebSocket update đến Manager sau khi transaction commit
        pushPreferenceUpdateToManagers(results);

        return results;
    }

    @Override
    @Transactional
    public ShiftPreferenceResponse updatePreference(Long id, ShiftPreferenceRequest request) {
        ShiftPreferenceEntity entity = shiftPreferenceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy nguyện vọng ca làm việc",
                        ErrorCode.SHIFT_PREFERENCE_NOT_FOUND));

        // Chỉ cho phép cập nhật khi status = PENDING
        if (entity.getStatus() != PreferenceStatus.PENDING) {
            throw new ConflictException(
                    "Nguyện vọng đã được áp dụng, không thể sửa đổi",
                    ErrorCode.SHIFT_PREFERENCE_ALREADY_APPLIED);
        }

        // Validate tuần chưa qua
        validateWeekNotPassed(request.getYear(), request.getWeekNumber());

        // Validate shift template nếu có
        ShiftTemplateEntity template = null;
        if (request.getShiftTemplateId() != null) {
            template = shiftTemplateRepository.findByIdAndDeletedFalse(request.getShiftTemplateId())
                    .filter(ShiftTemplateEntity::getIsActive)
                    .orElseThrow(() -> new NotFoundException(
                            "Mẫu ca không tồn tại hoặc đã bị vô hiệu",
                            ErrorCode.SHIFT_TEMPLATE_INACTIVE));
        }

        // Validate custom time nếu có
        if (request.getCustomStartTime() != null && request.getCustomEndTime() != null) {
            if (!request.getCustomStartTime().isBefore(request.getCustomEndTime())) {
                throw new BadRequestException(
                        "Giờ bắt đầu phải trước giờ kết thúc",
                        ErrorCode.SHIFT_PREFERENCE_INVALID_CUSTOM_TIME);
            }
        }

        // Cập nhật entity
        entity.setYear(request.getYear());
        entity.setWeekNumber(request.getWeekNumber());
        entity.setShiftTemplateId(request.getShiftTemplateId());
        entity.setCustomStartTime(request.getCustomStartTime());
        entity.setCustomEndTime(request.getCustomEndTime());
        entity.setReason(request.getReason());
        entity.setPriority(request.getReason() != null && !request.getReason().isBlank()
                ? PreferencePriority.HIGH
                : PreferencePriority.NORMAL);

        // Cập nhật dayOfWeek nếu request có daysOfWeek
        if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
            Integer dayOfWeek = request.getDaysOfWeek().get(0);
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                throw new BadRequestException(
                        "Ngày trong tuần không hợp lệ",
                        ErrorCode.SHIFT_PREFERENCE_INVALID_DAY);
            }
            entity.setDayOfWeek(dayOfWeek);
        }

        ShiftPreferenceEntity saved = shiftPreferenceRepository.save(entity);
        UserEntity employee = userRepository.findByIdAndDeletedFalse(entity.getEmployeeId()).orElse(null);

        ShiftPreferenceResponse response = shiftPreferenceMapper.toResponse(saved, employee, template);

        // Push WebSocket update đến Manager sau khi transaction commit
        pushPreferenceUpdateToManagers(List.of(response));

        return response;
    }

    @Override
    @Transactional
    public void deletePreference(Long id) {
        ShiftPreferenceEntity entity = shiftPreferenceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy nguyện vọng ca làm việc",
                        ErrorCode.SHIFT_PREFERENCE_NOT_FOUND));

        // Chỉ cho phép xóa khi status = PENDING
        if (entity.getStatus() != PreferenceStatus.PENDING) {
            throw new ConflictException(
                    "Nguyện vọng đã được áp dụng, không thể sửa đổi",
                    ErrorCode.SHIFT_PREFERENCE_ALREADY_APPLIED);
        }

        shiftPreferenceRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftPreferenceResponse> getPreferencesByWeek(Integer year, Integer weekNumber) {
        List<ShiftPreferenceEntity> entities = shiftPreferenceRepository
                .findByYearAndWeekNumber(year, weekNumber);

        return entities.stream()
                .map(this::toResponseWithLookup)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftPreferenceResponse> getPreferencesByWeeks(Integer year, List<Integer> weekNumbers) {
        List<ShiftPreferenceEntity> entities = shiftPreferenceRepository
                .findByYearAndWeekNumberIn(year, weekNumbers);

        return entities.stream()
                .map(this::toResponseWithLookup)
                .toList();
    }

    @Override
    @Transactional
    public void revertPreference(Long id) {
        ShiftPreferenceEntity entity = shiftPreferenceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy nguyện vọng ca làm việc",
                        ErrorCode.SHIFT_PREFERENCE_NOT_FOUND));

        if (entity.getStatus() != PreferenceStatus.APPLIED) {
            throw new BadRequestException(
                    "Nguyện vọng chưa được áp dụng, không thể hoàn tác",
                    ErrorCode.INVALID_REQUEST);
        }

        // Xóa assignment đã tạo
        if (entity.getAppliedAssignmentId() != null) {
            shiftAssignmentRepository.deleteById(entity.getAppliedAssignmentId());
        }

        // Đặt lại status PENDING
        entity.setStatus(PreferenceStatus.PENDING);
        entity.setAppliedAssignmentId(null);
        shiftPreferenceRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftPreferenceResponse> getMyPreferences(Long employeeId, Integer year, Integer weekNumber) {
        List<ShiftPreferenceEntity> entities = shiftPreferenceRepository
                .findByEmployeeIdAndYearAndWeekNumber(employeeId, year, weekNumber);

        return entities.stream()
                .map(this::toResponseWithLookup)
                .toList();
    }

    @Override
    @Transactional
    public ShiftPreferenceResponse applyPreference(Long id, ApplyPreferenceRequest applyRequest) {
        ShiftPreferenceEntity entity = shiftPreferenceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy nguyện vọng ca làm việc",
                        ErrorCode.SHIFT_PREFERENCE_NOT_FOUND));

        // Chỉ cho phép apply khi status = PENDING
        if (entity.getStatus() != PreferenceStatus.PENDING) {
            throw new ConflictException(
                    "Nguyện vọng đã được áp dụng, không thể sửa đổi",
                    ErrorCode.SHIFT_PREFERENCE_ALREADY_APPLIED);
        }

        // Tính workDate từ year + weekNumber + dayOfWeek
        LocalDate workDate = calculateWorkDate(entity.getYear(), entity.getWeekNumber(), entity.getDayOfWeek());

        // Xác định shiftTemplateId để tạo assignment
        Long templateId = resolveTemplateId(entity, applyRequest);

        // Tạo ShiftAssignment
        ShiftAssignmentEntity assignment = new ShiftAssignmentEntity();
        assignment.setEmployeeId(entity.getEmployeeId());
        assignment.setShiftTemplateId(templateId);
        assignment.setWorkDate(workDate);
        assignment.setStatus(ShiftAssignmentStatus.SCHEDULED);
        ShiftAssignmentEntity savedAssignment = shiftAssignmentRepository.save(assignment);

        // Cập nhật status nguyện vọng → APPLIED
        entity.setStatus(PreferenceStatus.APPLIED);
        entity.setAppliedAssignmentId(savedAssignment.getId());
        ShiftPreferenceEntity saved = shiftPreferenceRepository.save(entity);

        UserEntity employee = userRepository.findByIdAndDeletedFalse(entity.getEmployeeId()).orElse(null);
        ShiftTemplateEntity template = templateId != null
                ? shiftTemplateRepository.findByIdAndDeletedFalse(templateId).orElse(null)
                : null;

        return shiftPreferenceMapper.toResponse(saved, employee, template);
    }

    // === Private helper methods ===

    /**
     * Validate tuần chưa qua (dựa trên ISO week).
     */
    private void validateWeekNotPassed(Integer year, Integer weekNumber) {
        WeekFields weekFields = WeekFields.ISO;
        LocalDate now = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        int currentYear = now.get(weekFields.weekBasedYear());
        int currentWeek = now.get(weekFields.weekOfWeekBasedYear());

        if (year < currentYear || (year == currentYear && weekNumber < currentWeek)) {
            throw new BadRequestException(
                    "Không thể tạo nguyện vọng cho tuần đã qua",
                    ErrorCode.SHIFT_PREFERENCE_PAST_WEEK);
        }
    }

    /**
     * Kiểm tra trùng lặp nguyện vọng.
     */
    private void checkDuplicate(Long employeeId, ShiftPreferenceRequest request, Integer dayOfWeek) {
        Optional<ShiftPreferenceEntity> existing;

        if (request.getShiftTemplateId() != null) {
            existing = shiftPreferenceRepository
                    .findByEmployeeIdAndYearAndWeekNumberAndDayOfWeekAndShiftTemplateId(
                            employeeId, request.getYear(), request.getWeekNumber(),
                            dayOfWeek, request.getShiftTemplateId());
        } else {
            existing = shiftPreferenceRepository
                    .findByEmployeeIdAndYearAndWeekNumberAndDayOfWeekAndCustomStartTimeAndCustomEndTime(
                            employeeId, request.getYear(), request.getWeekNumber(),
                            dayOfWeek, request.getCustomStartTime(), request.getCustomEndTime());
        }

        if (existing.isPresent()) {
            throw new ConflictException(
                    "Nguyện vọng đã tồn tại cho ca này",
                    ErrorCode.SHIFT_PREFERENCE_DUPLICATE);
        }
    }

    /**
     * Tính ngày làm việc từ year, weekNumber, dayOfWeek (ISO 8601).
     */
    private LocalDate calculateWorkDate(Integer year, Integer weekNumber, Integer dayOfWeek) {
        WeekFields weekFields = WeekFields.ISO;
        // Lấy ngày đầu tuần (Monday) của tuần đó
        LocalDate monday = LocalDate.of(year, 1, 4) // Jan 4 luôn thuộc tuần 1 theo ISO
                .with(weekFields.weekOfWeekBasedYear(), weekNumber)
                .with(weekFields.dayOfWeek(), 1); // Monday
        // dayOfWeek: 1=Monday..7=Sunday
        return monday.plusDays(dayOfWeek - 1);
    }

    /**
     * Xác định shiftTemplateId dựa trên ApplyMode.
     */
    private Long resolveTemplateId(ShiftPreferenceEntity entity, ApplyPreferenceRequest applyRequest) {
        if (entity.getShiftTemplateId() != null) {
            // Nguyện vọng có template → dùng template đó
            return entity.getShiftTemplateId();
        }

        // Custom time → xử lý theo mode
        if (applyRequest.getMode() == ApplyMode.CREATE_NEW) {
            if (applyRequest.getNewTemplateId() != null) {
                return applyRequest.getNewTemplateId();
            }
            // Tự tạo template mới từ customStartTime/customEndTime
            return createTemplateFromPreference(entity, applyRequest.getNewTemplateName());
        }

        if (applyRequest.getMode() == ApplyMode.EXISTING_TEMPLATES
                && applyRequest.getTemplateIds() != null
                && !applyRequest.getTemplateIds().isEmpty()) {
            return applyRequest.getTemplateIds().get(0);
        }

        if (applyRequest.getMode() == ApplyMode.HYBRID
                && applyRequest.getTemplateIds() != null
                && !applyRequest.getTemplateIds().isEmpty()) {
            return applyRequest.getTemplateIds().get(0);
        }

        throw new BadRequestException(
                "Yêu cầu không hợp lệ",
                ErrorCode.INVALID_REQUEST);
    }

    /**
     * Tạo ShiftTemplate mới từ customStartTime/customEndTime của nguyện vọng.
     */
    private Long createTemplateFromPreference(ShiftPreferenceEntity entity, String templateName) {
        if (entity.getCustomStartTime() == null || entity.getCustomEndTime() == null) {
            throw new BadRequestException(
                    "Nguyện vọng không có thời gian tùy chỉnh để tạo mẫu ca",
                    ErrorCode.INVALID_REQUEST);
        }

        // Dùng tên từ frontend nếu có, fallback sang tên mặc định
        String name = (templateName != null && !templateName.isBlank())
                ? ensureUniqueName(templateName.trim())
                : ensureUniqueName(entity.getCustomStartTime().toString().substring(0, 5)
                        + "-" + entity.getCustomEndTime().toString().substring(0, 5));

        ShiftTemplateEntity template = new ShiftTemplateEntity();
        template.setName(name);
        template.setStartTime(entity.getCustomStartTime());
        template.setEndTime(entity.getCustomEndTime());
        template.setIsActive(true);
        template.setDeleted(false);

        ShiftTemplateEntity saved = shiftTemplateRepository.save(template);
        return saved.getId();
    }

    /**
     * Đảm bảo tên template không trùng, thêm số thứ tự nếu cần.
     */
    private String ensureUniqueName(String baseName) {
        String candidateName = baseName;
        int counter = 2;
        while (shiftTemplateRepository.existsByNameAndDeletedFalse(candidateName)) {
            candidateName = baseName + " " + counter;
            counter++;
        }
        return candidateName;
    }

    /**
     * Chuyển entity sang response với lookup employee và template.
     */
    private ShiftPreferenceResponse toResponseWithLookup(ShiftPreferenceEntity entity) {
        UserEntity employee = userRepository.findByIdAndDeletedFalse(entity.getEmployeeId()).orElse(null);
        ShiftTemplateEntity template = entity.getShiftTemplateId() != null
                ? shiftTemplateRepository.findByIdAndDeletedFalse(entity.getShiftTemplateId()).orElse(null)
                : null;
        return shiftPreferenceMapper.toResponse(entity, employee, template);
    }

    // === WebSocket push methods ===

    /**
     * Push danh sách preference updates đến tất cả Manager/Admin trong tenant hiện tại.
     * Sử dụng pattern pushAfterCommit từ NotificationServiceImpl để đảm bảo
     * data đã được commit trước khi frontend nhận WebSocket message và fetch lại.
     */
    private void pushPreferenceUpdateToManagers(List<ShiftPreferenceResponse> responses) {
        // Lấy danh sách Manager/Admin IDs trước khi register synchronization
        List<Long> managerIds = userRepository.findByRoleInAndDeletedFalse(MANAGER_ROLES)
                .stream()
                .map(UserEntity::getId)
                .toList();

        if (managerIds.isEmpty()) {
            log.debug("Không tìm thấy Manager/Admin nào để push shift preference update");
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (Long managerId : managerIds) {
                        pushPreferenceToUser(managerId, responses);
                    }
                }
            });
        } else {
            // Không có transaction active, push ngay
            for (Long managerId : managerIds) {
                pushPreferenceToUser(managerId, responses);
            }
        }
    }

    /**
     * Push preference update đến một user cụ thể qua WebSocket.
     */
    private void pushPreferenceToUser(Long userId, List<ShiftPreferenceResponse> responses) {
        try {
            log.info("Push shift preference update đến user {}: {} preferences", userId, responses.size());

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    PREFERENCE_UPDATE_DESTINATION,
                    responses);

            log.info("Đã push shift preference update qua WebSocket cho user {}", userId);
        } catch (Exception e) {
            // Log warning nhưng không throw exception để không ảnh hưởng flow chính
            log.warn("Không thể push shift preference update qua WebSocket cho user {}: {}",
                    userId, e.getMessage(), e);
        }
    }
}
