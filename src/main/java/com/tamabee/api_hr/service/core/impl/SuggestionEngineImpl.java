package com.tamabee.api_hr.service.core.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.response.attendance.CustomTimeResolution;
import com.tamabee.api_hr.dto.response.attendance.EmployeePreferenceSummary;
import com.tamabee.api_hr.dto.response.attendance.ShiftSuggestion;
import com.tamabee.api_hr.dto.response.attendance.SuggestionResponse;
import com.tamabee.api_hr.dto.response.attendance.TemplateMatch;
import com.tamabee.api_hr.dto.response.attendance.TimeGap;
import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.entity.attendance.ShiftPreferenceEntity;
import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.PreferencePriority;
import com.tamabee.api_hr.enums.UserRole;
import com.tamabee.api_hr.mapper.company.ShiftMapper;
import com.tamabee.api_hr.mapper.company.ShiftPreferenceMapper;
import com.tamabee.api_hr.repository.attendance.ShiftAssignmentRepository;
import com.tamabee.api_hr.repository.attendance.ShiftPreferenceRepository;
import com.tamabee.api_hr.repository.attendance.ShiftTemplateRepository;
import com.tamabee.api_hr.repository.leave.HolidayRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.cache.ICachedCompanySettingsService;
import com.tamabee.api_hr.service.core.interfaces.ISuggestionEngine;
import com.tamabee.api_hr.util.RegionUtil;

import lombok.RequiredArgsConstructor;

/**
 * Implementation của SuggestionEngine.
 * Gợi ý phân ca dựa trên nguyện vọng + dữ liệu lịch sử + Company_Setting.
 */
@Service
@RequiredArgsConstructor
public class SuggestionEngineImpl implements ISuggestionEngine {

    private static final int DEFAULT_WEEKS_BACK = 4;

    private final ShiftPreferenceRepository shiftPreferenceRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final HolidayRepository holidayRepository;
    private final UserRepository userRepository;
    private final ICachedCompanySettingsService cachedCompanySettingsService;
    private final ShiftPreferenceMapper shiftPreferenceMapper;
    private final ShiftMapper shiftMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SuggestionResponse> getSuggestions(Integer year, Integer weekNumber) {
        // Lấy tất cả shift templates active
        List<ShiftTemplateEntity> activeTemplates = shiftTemplateRepository.findByIsActiveTrueAndDeletedFalse();

        // Lấy nguyện vọng cho tuần này
        List<ShiftPreferenceEntity> preferences = shiftPreferenceRepository
                .findByYearAndWeekNumber(year, weekNumber);

        // Không có template VÀ không có nguyện vọng → trả rỗng
        if (activeTemplates.isEmpty() && preferences.isEmpty()) {
            return List.of();
        }

        // Lấy tất cả employees (EMPLOYEE_COMPANY)
        List<UserEntity> allEmployees = userRepository
                .findByRoleInAndDeletedFalse(List.of(UserRole.EMPLOYEE_COMPANY));

        // Lấy Company_Setting để xác định ngày nghỉ
        AttendanceConfig attendanceConfig = cachedCompanySettingsService.getAttendanceConfig();

        // Tính date range cho tuần
        LocalDate weekStart = calculateWeekStart(year, weekNumber);
        LocalDate weekEnd = weekStart.plusDays(6);

        // Lấy holidays trong tuần
        Set<LocalDate> holidayDates = holidayRepository.findByDateBetween(weekStart, weekEnd)
                .stream()
                .map(h -> h.getDate())
                .collect(Collectors.toSet());

        // Nhóm nguyện vọng theo dayOfWeek
        Map<Integer, List<ShiftPreferenceEntity>> preferencesByDay = preferences.stream()
                .collect(Collectors.groupingBy(ShiftPreferenceEntity::getDayOfWeek));

        // Tập hợp employeeIds có nguyện vọng theo ngày
        Map<Integer, Set<Long>> employeesWithPreferenceByDay = new HashMap<>();
        for (var entry : preferencesByDay.entrySet()) {
            employeesWithPreferenceByDay.put(entry.getKey(),
                    entry.getValue().stream()
                            .map(ShiftPreferenceEntity::getEmployeeId)
                            .collect(Collectors.toSet()));
        }

        // Tạo map employee theo ID để lookup nhanh
        Map<Long, UserEntity> employeeMap = allEmployees.stream()
                .collect(Collectors.toMap(UserEntity::getId, e -> e));

        List<SuggestionResponse> results = new ArrayList<>();

        // Duyệt qua 7 ngày trong tuần (1=Monday..7=Sunday)
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            LocalDate date = weekStart.plusDays(dayOfWeek - 1);

            // Loại trừ ngày nghỉ lễ
            if (holidayDates.contains(date)) {
                continue;
            }

            // Loại trừ cuối tuần theo Company_Setting (chỉ khi không có nguyện vọng)
            List<ShiftPreferenceEntity> dayPreferences = preferencesByDay.getOrDefault(dayOfWeek, List.of());
            if (isWeekendOff(dayOfWeek, attendanceConfig) && dayPreferences.isEmpty()) {
                continue;
            }

            // Phân tích lịch sử cho ngày này
            Map<Long, Integer> historicalCounts = getHistoricalCountsByTemplate(dayOfWeek, DEFAULT_WEEKS_BACK);

            // Tập hợp employees có nguyện vọng cho ngày này
            Set<Long> employeesWithPref = employeesWithPreferenceByDay.getOrDefault(dayOfWeek, Set.of());

            // Tạo gợi ý cho mỗi shift template
            List<ShiftSuggestion> suggestions = new ArrayList<>();
            for (ShiftTemplateEntity template : activeTemplates) {
                ShiftSuggestion suggestion = buildSuggestion(
                        template, dayPreferences, employeesWithPref,
                        allEmployees, employeeMap, historicalCounts);
                suggestions.add(suggestion);
            }

            // Tạo gợi ý cho nguyện vọng custom time (không gắn template)
            List<ShiftPreferenceEntity> customPreferences = dayPreferences.stream()
                    .filter(p -> p.getShiftTemplateId() == null && p.getCustomStartTime() != null)
                    .toList();
            if (!customPreferences.isEmpty()) {
                ShiftSuggestion customSuggestion = buildCustomTimeSuggestion(
                        customPreferences, employeeMap);
                suggestions.add(customSuggestion);
            }

            SuggestionResponse response = new SuggestionResponse();
            response.setDayOfWeek(dayOfWeek);
            response.setDate(date);
            response.setSuggestions(suggestions);
            results.add(response);
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomTimeResolution findMatchingTemplates(LocalTime customStart, LocalTime customEnd) {
        // Lấy tất cả active templates
        List<ShiftTemplateEntity> activeTemplates = shiftTemplateRepository.findByIsActiveTrueAndDeletedFalse();

        long customDurationMinutes = ChronoUnit.MINUTES.between(customStart, customEnd);

        // Tìm templates có overlap với custom time
        List<TemplateMatch> matchingTemplates = new ArrayList<>();
        for (ShiftTemplateEntity template : activeTemplates) {
            LocalTime overlapStart = template.getStartTime().isAfter(customStart)
                    ? template.getStartTime() : customStart;
            LocalTime overlapEnd = template.getEndTime().isBefore(customEnd)
                    ? template.getEndTime() : customEnd;

            // Kiểm tra có overlap không
            if (overlapStart.isBefore(overlapEnd)) {
                long overlapMinutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                double overlapPercentage = customDurationMinutes > 0
                        ? (double) overlapMinutes / customDurationMinutes * 100.0
                        : 0.0;

                TemplateMatch match = new TemplateMatch();
                match.setTemplate(shiftMapper.toResponse(template));
                match.setCoverStart(overlapStart);
                match.setCoverEnd(overlapEnd);
                match.setOverlapPercentage(overlapPercentage);
                matchingTemplates.add(match);
            }
        }

        // Sắp xếp theo coverStart
        matchingTemplates.sort(Comparator.comparing(TemplateMatch::getCoverStart));

        // Tính uncovered gaps
        List<TimeGap> uncoveredGaps = calculateGaps(customStart, customEnd, matchingTemplates);

        // Xác định fullyCovered
        boolean fullyCovered = uncoveredGaps.isEmpty() && !matchingTemplates.isEmpty();

        CustomTimeResolution resolution = new CustomTimeResolution();
        resolution.setMatchingTemplates(matchingTemplates);
        resolution.setUncoveredGaps(uncoveredGaps);
        resolution.setFullyCovered(fullyCovered);

        return resolution;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer analyzeWeekdayPatterns(Integer dayOfWeek, Integer weeksBack) {
        LocalDate now = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        int totalEmployees = 0;
        int weekCount = 0;

        // Lấy theo từng ngày matching dayOfWeek trong weeksBack tuần gần nhất
        for (int w = 1; w <= weeksBack; w++) {
            LocalDate targetDate = findDateForDayOfWeek(now.minusWeeks(w), dayOfWeek);
            if (targetDate.isAfter(now)) {
                continue;
            }

            List<ShiftAssignmentEntity> dayAssignments = shiftAssignmentRepository
                    .findByWorkDate(targetDate);

            // Đếm số nhân viên unique trong ngày đó
            long uniqueEmployees = dayAssignments.stream()
                    .map(ShiftAssignmentEntity::getEmployeeId)
                    .distinct()
                    .count();

            totalEmployees += (int) uniqueEmployees;
            weekCount++;
        }

        return weekCount > 0 ? Math.round((float) totalEmployees / weekCount) : 0;
    }

    @Override
    @Transactional
    public void copyScheduleFromPeriod(Integer sourceYear, Integer sourceWeek,
            Integer targetYear, Integer targetWeek) {
        // Tính date range cho tuần nguồn và tuần đích
        LocalDate sourceWeekStart = calculateWeekStart(sourceYear, sourceWeek);
        LocalDate targetWeekStart = calculateWeekStart(targetYear, targetWeek);

        // Duyệt qua 7 ngày trong tuần (dayOfWeek mapping)
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate sourceDate = sourceWeekStart.plusDays(dayOffset);
            LocalDate targetDate = targetWeekStart.plusDays(dayOffset);

            // Lấy assignments từ ngày nguồn
            List<ShiftAssignmentEntity> sourceAssignments = shiftAssignmentRepository
                    .findByWorkDate(sourceDate);

            // Tạo assignments mới cho ngày đích
            for (ShiftAssignmentEntity source : sourceAssignments) {
                ShiftAssignmentEntity newAssignment = new ShiftAssignmentEntity();
                newAssignment.setEmployeeId(source.getEmployeeId());
                newAssignment.setShiftTemplateId(source.getShiftTemplateId());
                newAssignment.setWorkDate(targetDate);
                newAssignment.setStatus(source.getStatus());
                shiftAssignmentRepository.save(newAssignment);
            }
        }
    }

    // === Private helper methods ===

    /**
     * Tính ngày đầu tuần (Monday) từ year và weekNumber (ISO 8601).
     */
    private LocalDate calculateWeekStart(Integer year, Integer weekNumber) {
        WeekFields weekFields = WeekFields.ISO;
        return LocalDate.of(year, 1, 4) // Jan 4 luôn thuộc tuần 1 theo ISO
                .with(weekFields.weekOfWeekBasedYear(), weekNumber)
                .with(weekFields.dayOfWeek(), 1); // Monday
    }

    /**
     * Kiểm tra ngày có phải cuối tuần nghỉ theo Company_Setting không.
     */
    private boolean isWeekendOff(int dayOfWeek, AttendanceConfig config) {
        // dayOfWeek: 6=Saturday, 7=Sunday (ISO)
        if (dayOfWeek == 6 && Boolean.TRUE.equals(config.getSaturdayOff())) {
            return true;
        }
        if (dayOfWeek == 7 && Boolean.TRUE.equals(config.getSundayOff())) {
            return true;
        }
        return false;
    }

    /**
     * Lấy historical counts theo template cho một dayOfWeek.
     * Nhóm theo dayOfWeek (1-7), tính trung bình số nhân viên per template.
     */
    private Map<Long, Integer> getHistoricalCountsByTemplate(int dayOfWeek, int weeksBack) {
        LocalDate now = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        Map<Long, Integer> totalByTemplate = new HashMap<>();
        Map<Long, Integer> weekCountByTemplate = new HashMap<>();

        for (int w = 1; w <= weeksBack; w++) {
            LocalDate targetDate = findDateForDayOfWeek(now.minusWeeks(w), dayOfWeek);

            List<ShiftAssignmentEntity> dayAssignments = shiftAssignmentRepository
                    .findByWorkDate(targetDate);

            // Nhóm theo shiftTemplateId, đếm unique employees
            Map<Long, Long> employeeCountByTemplate = dayAssignments.stream()
                    .collect(Collectors.groupingBy(
                            ShiftAssignmentEntity::getShiftTemplateId,
                            Collectors.mapping(ShiftAssignmentEntity::getEmployeeId,
                                    Collectors.collectingAndThen(Collectors.toSet(), s -> (long) s.size()))));

            for (var entry : employeeCountByTemplate.entrySet()) {
                totalByTemplate.merge(entry.getKey(), entry.getValue().intValue(), Integer::sum);
                weekCountByTemplate.merge(entry.getKey(), 1, Integer::sum);
            }
        }

        // Tính trung bình
        Map<Long, Integer> averages = new HashMap<>();
        for (var entry : totalByTemplate.entrySet()) {
            int count = weekCountByTemplate.getOrDefault(entry.getKey(), 1);
            averages.put(entry.getKey(), Math.round((float) entry.getValue() / count));
        }

        return averages;
    }

    /**
     * Tìm ngày cụ thể cho dayOfWeek trong tuần chứa referenceDate.
     */
    private LocalDate findDateForDayOfWeek(LocalDate referenceDate, int dayOfWeek) {
        // dayOfWeek: 1=Monday..7=Sunday (ISO)
        DayOfWeek targetDay = DayOfWeek.of(dayOfWeek);
        // Tìm ngày trong tuần chứa referenceDate
        LocalDate monday = referenceDate.with(DayOfWeek.MONDAY);
        return monday.plusDays(targetDay.getValue() - 1);
    }

    /**
     * Xây dựng ShiftSuggestion cho một template cụ thể.
     */
    private ShiftSuggestion buildSuggestion(
            ShiftTemplateEntity template,
            List<ShiftPreferenceEntity> dayPreferences,
            Set<Long> employeesWithPref,
            List<UserEntity> allEmployees,
            Map<Long, UserEntity> employeeMap,
            Map<Long, Integer> historicalCounts) {

        // Lọc nguyện vọng cho template này
        List<ShiftPreferenceEntity> templatePreferences = dayPreferences.stream()
                .filter(p -> template.getId().equals(p.getShiftTemplateId()))
                .toList();

        // Tạo preferred employees (có nguyện vọng cho template này)
        List<EmployeePreferenceSummary> preferredEmployees = templatePreferences.stream()
                .map(p -> shiftPreferenceMapper.toSummary(p, employeeMap.get(p.getEmployeeId())))
                .sorted(Comparator.comparing(
                        (EmployeePreferenceSummary s) -> s.getPriority() == PreferencePriority.HIGH ? 0 : 1))
                .toList();

        // Tạo flexible employees (không có nguyện vọng cho ngày này)
        List<EmployeePreferenceSummary> flexibleEmployees = allEmployees.stream()
                .filter(e -> !employeesWithPref.contains(e.getId()))
                .map(e -> {
                    EmployeePreferenceSummary summary = new EmployeePreferenceSummary();
                    summary.setEmployeeId(e.getId());
                    summary.setEmployeeName(
                            e.getProfile() != null ? e.getProfile().getName() : null);
                    summary.setPriority(PreferencePriority.NORMAL);
                    return summary;
                })
                .toList();

        // Historical count cho template này
        Integer historicalCount = historicalCounts.getOrDefault(template.getId(), 0);

        ShiftSuggestion suggestion = new ShiftSuggestion();
        suggestion.setShiftTemplate(shiftMapper.toResponse(template));
        suggestion.setPreferredEmployees(preferredEmployees);
        suggestion.setFlexibleEmployees(flexibleEmployees);
        suggestion.setHistoricalCount(historicalCount);

        return suggestion;
    }

    /**
     * Tạo gợi ý cho nguyện vọng custom time (không gắn template).
     * shiftTemplate = null, hiển thị thời gian tùy chỉnh của từng nhân viên.
     */
    private ShiftSuggestion buildCustomTimeSuggestion(
            List<ShiftPreferenceEntity> customPreferences,
            Map<Long, UserEntity> employeeMap) {

        List<EmployeePreferenceSummary> preferredEmployees = customPreferences.stream()
                .map(p -> {
                    UserEntity user = employeeMap.get(p.getEmployeeId());
                    EmployeePreferenceSummary summary = new EmployeePreferenceSummary();
                    summary.setEmployeeId(p.getEmployeeId());
                    summary.setEmployeeName(user != null && user.getProfile() != null
                            ? user.getProfile().getName() : null);
                    summary.setReason(p.getReason());
                    summary.setPriority(p.getPriority());
                    summary.setCustomStartTime(p.getCustomStartTime() != null
                            ? p.getCustomStartTime().toString() : null);
                    summary.setCustomEndTime(p.getCustomEndTime() != null
                            ? p.getCustomEndTime().toString() : null);
                    return summary;
                })
                .sorted(Comparator.comparing(
                        (EmployeePreferenceSummary s) -> s.getPriority() == PreferencePriority.HIGH ? 0 : 1))
                .toList();

        ShiftSuggestion suggestion = new ShiftSuggestion();
        suggestion.setShiftTemplate(null);
        suggestion.setPreferredEmployees(preferredEmployees);
        suggestion.setFlexibleEmployees(List.of());
        suggestion.setHistoricalCount(0);

        return suggestion;
    }

    /**
     * Tính các khoảng thời gian chưa được bao phủ bởi templates.
     */
    private List<TimeGap> calculateGaps(LocalTime customStart, LocalTime customEnd,
            List<TemplateMatch> sortedMatches) {
        List<TimeGap> gaps = new ArrayList<>();

        if (sortedMatches.isEmpty()) {
            // Không có template nào → toàn bộ custom time là gap
            TimeGap gap = new TimeGap();
            gap.setStart(customStart);
            gap.setEnd(customEnd);
            gaps.add(gap);
            return gaps;
        }

        // Merge overlapping coverage intervals
        List<LocalTime[]> mergedIntervals = mergeIntervals(sortedMatches);

        // Tìm gaps giữa customStart và interval đầu tiên
        LocalTime currentPos = customStart;
        for (LocalTime[] interval : mergedIntervals) {
            if (currentPos.isBefore(interval[0])) {
                TimeGap gap = new TimeGap();
                gap.setStart(currentPos);
                gap.setEnd(interval[0]);
                gaps.add(gap);
            }
            if (interval[1].isAfter(currentPos)) {
                currentPos = interval[1];
            }
        }

        // Gap sau interval cuối cùng đến customEnd
        if (currentPos.isBefore(customEnd)) {
            TimeGap gap = new TimeGap();
            gap.setStart(currentPos);
            gap.setEnd(customEnd);
            gaps.add(gap);
        }

        return gaps;
    }

    /**
     * Merge overlapping intervals từ template matches.
     */
    private List<LocalTime[]> mergeIntervals(List<TemplateMatch> sortedMatches) {
        List<LocalTime[]> intervals = sortedMatches.stream()
                .map(m -> new LocalTime[] { m.getCoverStart(), m.getCoverEnd() })
                .sorted(Comparator.comparing(a -> a[0]))
                .collect(Collectors.toList());

        List<LocalTime[]> merged = new ArrayList<>();
        for (LocalTime[] interval : intervals) {
            if (merged.isEmpty() || merged.get(merged.size() - 1)[1].isBefore(interval[0])) {
                merged.add(new LocalTime[] { interval[0], interval[1] });
            } else {
                // Extend the last merged interval
                LocalTime[] last = merged.get(merged.size() - 1);
                if (interval[1].isAfter(last[1])) {
                    last[1] = interval[1];
                }
            }
        }

        return merged;
    }
}
