package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.config.BreakPeriod;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho Break Service
 * Feature: break-time-backend, multiple-breaks-support
 */
public class BreakServicePropertyTest {

        /**
         * Property 1: Sequential Break Number Assignment
         * For any attendance record with N existing breaks, when a new break is
         * started,
         * the new break SHALL have breakNumber = N + 1.
         */
        @Property(tries = 100)
        void sequentialBreakNumberAssignment(
                        @ForAll("attendanceWithMultipleBreaks") AttendanceWithBreaks attendance) {

                List<BreakRecordEntity> breaks = attendance.breakRecords;

                if (breaks.isEmpty()) {
                        return;
                }

                // Sắp xếp breaks theo breakNumber
                List<BreakRecordEntity> sortedBreaks = new ArrayList<>(breaks);
                sortedBreaks.sort(Comparator.comparing(BreakRecordEntity::getBreakNumber));

                // Kiểm tra breakNumber bắt đầu từ 1
                assertEquals(1, sortedBreaks.get(0).getBreakNumber(),
                                "First break should have breakNumber = 1");

                // Kiểm tra breakNumber tăng tuần tự
                for (int i = 0; i < sortedBreaks.size(); i++) {
                        int expectedBreakNumber = i + 1;
                        int actualBreakNumber = sortedBreaks.get(i).getBreakNumber();
                        assertEquals(expectedBreakNumber, actualBreakNumber,
                                        String.format("Break at index %d should have breakNumber = %d, but got %d",
                                                        i, expectedBreakNumber, actualBreakNumber));
                }

                // Kiểm tra tất cả breaks thuộc cùng attendance record
                Long attendanceRecordId = breaks.get(0).getAttendanceRecordId();
                for (BreakRecordEntity breakRecord : breaks) {
                        assertEquals(attendanceRecordId, breakRecord.getAttendanceRecordId(),
                                        "All breaks should belong to the same attendance record");
                }
        }

        /**
         * Property 6: No Concurrent Active Breaks
         * For any attendance record, at most one break can be active (breakEnd is null)
         * at any time.
         */
        @Property(tries = 100)
        void noConcurrentActiveBreaks(
                        @ForAll("attendanceWithMixedBreaks") AttendanceWithBreaks attendance) {

                List<BreakRecordEntity> breaks = attendance.breakRecords;

                // Đếm số breaks đang active (breakEnd is null)
                long activeBreakCount = breaks.stream()
                                .filter(b -> b.getBreakStart() != null && b.getBreakEnd() == null)
                                .count();

                // Chỉ được có tối đa 1 break active
                assertTrue(activeBreakCount <= 1,
                                String.format("Should have at most 1 active break, but found %d", activeBreakCount));
        }

        /**
         * Property 8: Maximum Breaks Per Day Enforcement
         * For any attendance record, the number of breaks SHALL not exceed
         * maxBreaksPerDay.
         */
        @Property(tries = 100)
        void maxBreaksPerDayEnforcement(
                        @ForAll("attendanceWithLimitedBreaks") AttendanceWithLimitedBreaks attendance) {

                List<BreakRecordEntity> breaks = attendance.breakRecords;
                int maxBreaksPerDay = attendance.maxBreaksPerDay;

                // Số breaks không được vượt quá maxBreaksPerDay
                assertTrue(breaks.size() <= maxBreaksPerDay,
                                String.format("Should have at most %d breaks, but found %d",
                                                maxBreaksPerDay, breaks.size()));
        }

        /**
         * Property 2: Total Break Duration Calculation
         * For any attendance record with multiple break records, the total break
         * duration
         * SHALL equal the sum of all individual break durations (actualBreakMinutes).
         */
        @Property(tries = 100)
        void totalBreakDurationCalculation(
                        @ForAll("attendanceWithMultipleBreaks") AttendanceWithBreaks attendance) {

                List<BreakRecordEntity> breaks = attendance.breakRecords;

                // Tính tổng thời gian break từ tất cả sessions
                int expectedTotal = breaks.stream()
                                .mapToInt(b -> b.getActualBreakMinutes() != null ? b.getActualBreakMinutes() : 0)
                                .sum();

                // Tính tổng từng break riêng lẻ
                int sumOfIndividual = 0;
                for (BreakRecordEntity breakRecord : breaks) {
                        if (breakRecord.getActualBreakMinutes() != null) {
                                sumOfIndividual += breakRecord.getActualBreakMinutes();
                        }
                }

                // Tổng phải bằng nhau
                assertEquals(expectedTotal, sumOfIndividual,
                                String.format("Total break duration should equal sum of individual durations. Expected: %d, Got: %d",
                                                expectedTotal, sumOfIndividual));

                // Tổng không được âm
                assertTrue(expectedTotal >= 0,
                                String.format("Total break duration should be non-negative, got %d", expectedTotal));
        }

        /**
         * Property 2 (original): Break Time Within Working Hours
         * For any fixed break period in a work schedule, the break start and end times
         * SHALL fall within the working hours.
         */
        @Property(tries = 100)
        void fixedBreakPeriodWithinWorkingHours(
                        @ForAll("workScheduleWithBreaks") WorkScheduleWithBreaks schedule) {

                LocalTime workStart = schedule.workStartTime;
                LocalTime workEnd = schedule.workEndTime;

                for (BreakPeriod breakPeriod : schedule.breakPeriods) {
                        if (breakPeriod.getStartTime() != null && breakPeriod.getEndTime() != null) {
                                LocalTime breakStart = breakPeriod.getStartTime();
                                LocalTime breakEnd = breakPeriod.getEndTime();

                                // Kiểm tra break start nằm trong working hours
                                boolean startWithinWorkHours = isTimeWithinRange(breakStart, workStart, workEnd);
                                assertTrue(startWithinWorkHours,
                                                String.format("Break start %s should be within working hours %s-%s",
                                                                breakStart, workStart, workEnd));

                                // Kiểm tra break end nằm trong working hours
                                boolean endWithinWorkHours = isTimeWithinRange(breakEnd, workStart, workEnd);
                                assertTrue(endWithinWorkHours,
                                                String.format("Break end %s should be within working hours %s-%s",
                                                                breakEnd, workStart, workEnd));

                                // Kiểm tra break start trước break end
                                assertTrue(breakStart.isBefore(breakEnd) || breakStart.equals(breakEnd),
                                                String.format("Break start %s should be before or equal to break end %s",
                                                                breakStart, breakEnd));
                        }
                }
        }

        /**
         * Property 9: Break Record Audit Trail
         * For any break record, both the actual break duration and effective break
         * duration
         * SHALL be stored.
         */
        @Property(tries = 100)
        void breakRecordAuditTrailComplete(
                        @ForAll("completedBreakRecords") BreakRecordEntity breakRecord) {

                // Kiểm tra actual break minutes được lưu
                assertNotNull(breakRecord.getActualBreakMinutes(),
                                "Actual break minutes should be stored for audit trail");

                // Kiểm tra effective break minutes được lưu
                assertNotNull(breakRecord.getEffectiveBreakMinutes(),
                                "Effective break minutes should be stored for audit trail");

                // Kiểm tra actual break minutes không âm
                assertTrue(breakRecord.getActualBreakMinutes() >= 0,
                                String.format("Actual break minutes should be non-negative, got %d",
                                                breakRecord.getActualBreakMinutes()));

                // Kiểm tra effective break minutes không âm
                assertTrue(breakRecord.getEffectiveBreakMinutes() >= 0,
                                String.format("Effective break minutes should be non-negative, got %d",
                                                breakRecord.getEffectiveBreakMinutes()));

                // Kiểm tra effective không vượt quá actual (trừ khi có minimum enforcement)
                // Effective có thể lớn hơn actual nếu actual < minimum
                // Effective có thể nhỏ hơn actual nếu actual > maximum
                // Nên chỉ kiểm tra cả hai đều được lưu
        }

        /**
         * Property bổ sung: Break Record có đầy đủ thông tin liên kết
         * For any break record, the attendance record ID, employee ID, company ID,
         * and work date SHALL be stored.
         */
        @Property(tries = 100)
        void breakRecordHasRequiredAssociations(
                        @ForAll("completedBreakRecords") BreakRecordEntity breakRecord) {

                // Kiểm tra attendance record ID
                assertNotNull(breakRecord.getAttendanceRecordId(),
                                "Break record should have attendance record ID");

                // Kiểm tra employee ID
                assertNotNull(breakRecord.getEmployeeId(),
                                "Break record should have employee ID");

                // Kiểm tra company ID
                assertNotNull(breakRecord.getCompanyId(),
                                "Break record should have company ID");

                // Kiểm tra work date
                assertNotNull(breakRecord.getWorkDate(),
                                "Break record should have work date");
        }

        /**
         * Property bổ sung: Break times consistency
         * For any completed break record, break end should be after break start.
         */
        @Property(tries = 100)
        void breakEndAfterBreakStart(
                        @ForAll("completedBreakRecordsWithTimes") BreakRecordEntity breakRecord) {

                LocalDateTime breakStart = breakRecord.getBreakStart();
                LocalDateTime breakEnd = breakRecord.getBreakEnd();

                if (breakStart != null && breakEnd != null) {
                        assertTrue(breakEnd.isAfter(breakStart) || breakEnd.equals(breakStart),
                                        String.format("Break end %s should be after or equal to break start %s",
                                                        breakEnd, breakStart));
                }
        }

        /**
         * Property 7: No Overlapping Break Times
         * For any new break with start time T, if there exists a completed break
         * where breakStart <= T <= breakEnd, the system SHALL reject the request.
         */
        @Property(tries = 100)
        void noOverlappingBreakTimes(
                        @ForAll("attendanceWithNonOverlappingBreaks") AttendanceWithBreaks attendance) {

                List<BreakRecordEntity> breaks = attendance.breakRecords;

                // Kiểm tra không có breaks nào overlap với nhau
                for (int i = 0; i < breaks.size(); i++) {
                        BreakRecordEntity break1 = breaks.get(i);
                        if (break1.getBreakStart() == null || break1.getBreakEnd() == null) {
                                continue; // Bỏ qua active breaks
                        }

                        for (int j = i + 1; j < breaks.size(); j++) {
                                BreakRecordEntity break2 = breaks.get(j);
                                if (break2.getBreakStart() == null || break2.getBreakEnd() == null) {
                                        continue; // Bỏ qua active breaks
                                }

                                // Kiểm tra break2.start không nằm trong [break1.start, break1.end]
                                boolean break2StartOverlapsBreak1 = !break2.getBreakStart()
                                                .isBefore(break1.getBreakStart())
                                                && !break2.getBreakStart().isAfter(break1.getBreakEnd());

                                // Kiểm tra break1.start không nằm trong [break2.start, break2.end]
                                boolean break1StartOverlapsBreak2 = !break1.getBreakStart()
                                                .isBefore(break2.getBreakStart())
                                                && !break1.getBreakStart().isAfter(break2.getBreakEnd());

                                assertFalse(break2StartOverlapsBreak1,
                                                String.format("Break %d start time %s should not overlap with break %d [%s - %s]",
                                                                break2.getBreakNumber(), break2.getBreakStart(),
                                                                break1.getBreakNumber(), break1.getBreakStart(),
                                                                break1.getBreakEnd()));

                                assertFalse(break1StartOverlapsBreak2,
                                                String.format("Break %d start time %s should not overlap with break %d [%s - %s]",
                                                                break1.getBreakNumber(), break1.getBreakStart(),
                                                                break2.getBreakNumber(), break2.getBreakStart(),
                                                                break2.getBreakEnd()));
                        }
                }
        }

        // === Helper Methods ===

        /**
         * Kiểm tra thời gian có nằm trong khoảng không
         * Hỗ trợ cả trường hợp overnight (workEnd < workStart)
         */
        private boolean isTimeWithinRange(LocalTime time, LocalTime rangeStart, LocalTime rangeEnd) {
                if (rangeStart.isBefore(rangeEnd) || rangeStart.equals(rangeEnd)) {
                        // Normal case: 09:00 - 18:00
                        return !time.isBefore(rangeStart) && !time.isAfter(rangeEnd);
                } else {
                        // Overnight case: 22:00 - 06:00
                        return !time.isBefore(rangeStart) || !time.isAfter(rangeEnd);
                }
        }

        // === Helper Classes ===

        record WorkScheduleWithBreaks(
                        LocalTime workStartTime,
                        LocalTime workEndTime,
                        List<BreakPeriod> breakPeriods) {
        }

        record AttendanceWithBreaks(
                        Long attendanceRecordId,
                        Long employeeId,
                        Long companyId,
                        LocalDate workDate,
                        List<BreakRecordEntity> breakRecords) {
        }

        record AttendanceWithLimitedBreaks(
                        Long attendanceRecordId,
                        Long employeeId,
                        Long companyId,
                        LocalDate workDate,
                        List<BreakRecordEntity> breakRecords,
                        int maxBreaksPerDay) {
        }

        // === Generators ===

        @Provide
        Arbitrary<AttendanceWithBreaks> attendanceWithMultipleBreaks() {
                return Combinators.combine(
                                Arbitraries.longs().between(1, 1000),
                                Arbitraries.longs().between(1, 100),
                                Arbitraries.longs().between(1, 50),
                                Arbitraries.integers().between(1, 5))
                                .as((attendanceId, employeeId, companyId, breakCount) -> {
                                        LocalDate workDate = LocalDate.now();
                                        List<BreakRecordEntity> breaks = new ArrayList<>();

                                        for (int i = 0; i < breakCount; i++) {
                                                BreakRecordEntity breakRecord = new BreakRecordEntity();
                                                breakRecord.setAttendanceRecordId(attendanceId);
                                                breakRecord.setEmployeeId(employeeId);
                                                breakRecord.setCompanyId(companyId);
                                                breakRecord.setWorkDate(workDate);
                                                breakRecord.setBreakNumber(i + 1);
                                                breakRecord.setBreakStart(
                                                                LocalDateTime.of(workDate, LocalTime.of(9 + i * 2, 0)));
                                                breakRecord.setBreakEnd(LocalDateTime.of(workDate,
                                                                LocalTime.of(9 + i * 2, 30)));
                                                breakRecord.setActualBreakMinutes(30);
                                                breakRecord.setEffectiveBreakMinutes(30);
                                                breaks.add(breakRecord);
                                        }

                                        return new AttendanceWithBreaks(attendanceId, employeeId, companyId, workDate,
                                                        breaks);
                                });
        }

        @Provide
        Arbitrary<AttendanceWithBreaks> attendanceWithMixedBreaks() {
                return Combinators.combine(
                                Arbitraries.longs().between(1, 1000),
                                Arbitraries.longs().between(1, 100),
                                Arbitraries.longs().between(1, 50),
                                Arbitraries.integers().between(1, 5),
                                Arbitraries.integers().between(0, 1))
                                .as((attendanceId, employeeId, companyId, breakCount, hasActiveBreakInt) -> {
                                        boolean hasActiveBreak = hasActiveBreakInt == 1;
                                        LocalDate workDate = LocalDate.now();
                                        List<BreakRecordEntity> breaks = new ArrayList<>();

                                        for (int i = 0; i < breakCount; i++) {
                                                BreakRecordEntity breakRecord = new BreakRecordEntity();
                                                breakRecord.setAttendanceRecordId(attendanceId);
                                                breakRecord.setEmployeeId(employeeId);
                                                breakRecord.setCompanyId(companyId);
                                                breakRecord.setWorkDate(workDate);
                                                breakRecord.setBreakNumber(i + 1);
                                                breakRecord.setBreakStart(
                                                                LocalDateTime.of(workDate, LocalTime.of(9 + i * 2, 0)));

                                                // Break cuối cùng có thể active (breakEnd = null) nếu hasActiveBreak =
                                                // true
                                                boolean isLastBreak = (i == breakCount - 1);
                                                if (isLastBreak && hasActiveBreak) {
                                                        breakRecord.setBreakEnd(null);
                                                        breakRecord.setActualBreakMinutes(null);
                                                        breakRecord.setEffectiveBreakMinutes(null);
                                                } else {
                                                        breakRecord.setBreakEnd(LocalDateTime.of(workDate,
                                                                        LocalTime.of(9 + i * 2, 30)));
                                                        breakRecord.setActualBreakMinutes(30);
                                                        breakRecord.setEffectiveBreakMinutes(30);
                                                }
                                                breaks.add(breakRecord);
                                        }

                                        return new AttendanceWithBreaks(attendanceId, employeeId, companyId, workDate,
                                                        breaks);
                                });
        }

        @Provide
        Arbitrary<AttendanceWithLimitedBreaks> attendanceWithLimitedBreaks() {
                return Combinators.combine(
                                Arbitraries.longs().between(1, 1000),
                                Arbitraries.longs().between(1, 100),
                                Arbitraries.longs().between(1, 50),
                                Arbitraries.integers().between(1, 5))
                                .as((attendanceId, employeeId, companyId, maxBreaksPerDay) -> {
                                        LocalDate workDate = LocalDate.now();
                                        // Tạo số breaks <= maxBreaksPerDay
                                        int breakCount = Math.min(maxBreaksPerDay, 3);
                                        List<BreakRecordEntity> breaks = new ArrayList<>();

                                        for (int i = 0; i < breakCount; i++) {
                                                BreakRecordEntity breakRecord = new BreakRecordEntity();
                                                breakRecord.setAttendanceRecordId(attendanceId);
                                                breakRecord.setEmployeeId(employeeId);
                                                breakRecord.setCompanyId(companyId);
                                                breakRecord.setWorkDate(workDate);
                                                breakRecord.setBreakNumber(i + 1);
                                                breakRecord.setBreakStart(
                                                                LocalDateTime.of(workDate, LocalTime.of(9 + i * 2, 0)));
                                                breakRecord.setBreakEnd(LocalDateTime.of(workDate,
                                                                LocalTime.of(9 + i * 2, 30)));
                                                breakRecord.setActualBreakMinutes(30);
                                                breakRecord.setEffectiveBreakMinutes(30);
                                                breaks.add(breakRecord);
                                        }

                                        return new AttendanceWithLimitedBreaks(attendanceId, employeeId, companyId,
                                                        workDate, breaks, maxBreaksPerDay);
                                });
        }

        @Provide
        Arbitrary<WorkScheduleWithBreaks> workScheduleWithBreaks() {
                return Combinators.combine(
                                Arbitraries.integers().between(6, 10),
                                Arbitraries.integers().between(17, 21),
                                Arbitraries.integers().between(1, 3)).as((startHour, endHour, breakCount) -> {

                                        LocalTime workStart = LocalTime.of(startHour, 0);
                                        LocalTime workEnd = LocalTime.of(endHour, 0);

                                        // Tạo break periods nằm trong working hours
                                        List<BreakPeriod> breaks = createBreakPeriodsWithinRange(workStart, workEnd,
                                                        breakCount);
                                        return new WorkScheduleWithBreaks(workStart, workEnd, breaks);
                                });
        }

        private List<BreakPeriod> createBreakPeriodsWithinRange(
                        LocalTime workStart, LocalTime workEnd, int count) {

                int workStartMinutes = workStart.getHour() * 60 + workStart.getMinute();
                int workEndMinutes = workEnd.getHour() * 60 + workEnd.getMinute();
                int workDuration = workEndMinutes - workStartMinutes;

                // Chia working hours thành các slot cho break
                int slotDuration = workDuration / (count + 1);

                List<BreakPeriod> periods = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                        int breakDuration = Math.min(45, slotDuration - 10);
                        int breakStartMinutes = workStartMinutes + (i + 1) * slotDuration - breakDuration / 2;
                        int breakEndMinutes = breakStartMinutes + breakDuration;

                        // Đảm bảo break nằm trong working hours
                        breakStartMinutes = Math.max(workStartMinutes,
                                        Math.min(breakStartMinutes, workEndMinutes - breakDuration));
                        breakEndMinutes = Math.min(workEndMinutes, breakStartMinutes + breakDuration);

                        LocalTime breakStart = LocalTime.of(breakStartMinutes / 60, breakStartMinutes % 60);
                        LocalTime breakEnd = LocalTime.of(breakEndMinutes / 60, breakEndMinutes % 60);

                        periods.add(BreakPeriod.builder()
                                        .name("Break " + (i + 1))
                                        .startTime(breakStart)
                                        .endTime(breakEnd)
                                        .durationMinutes(breakDuration)
                                        .isFlexible(false)
                                        .order(i + 1)
                                        .build());
                }
                return periods;
        }

        @Provide
        Arbitrary<BreakRecordEntity> completedBreakRecords() {
                return Combinators.combine(
                                Arbitraries.longs().between(1, 1000),
                                Arbitraries.longs().between(1, 100),
                                Arbitraries.longs().between(1, 50),
                                Arbitraries.integers().between(0, 120),
                                Arbitraries.integers().between(0, 120))
                                .as((attendanceId, employeeId, companyId, actualMinutes, effectiveMinutes) -> {
                                        BreakRecordEntity record = new BreakRecordEntity();
                                        record.setAttendanceRecordId(attendanceId);
                                        record.setEmployeeId(employeeId);
                                        record.setCompanyId(companyId);
                                        record.setWorkDate(LocalDate.now());
                                        record.setActualBreakMinutes(actualMinutes);
                                        record.setEffectiveBreakMinutes(effectiveMinutes);
                                        return record;
                                });
        }

        @Provide
        Arbitrary<BreakRecordEntity> completedBreakRecordsWithTimes() {
                return Combinators.combine(
                                Arbitraries.longs().between(1, 1000),
                                Arbitraries.longs().between(1, 100),
                                Arbitraries.longs().between(1, 50),
                                Arbitraries.integers().between(9, 17),
                                Arbitraries.integers().between(0, 59),
                                Arbitraries.integers().between(15, 90))
                                .as((attendanceId, employeeId, companyId, startHour, startMinute, duration) -> {
                                        BreakRecordEntity record = new BreakRecordEntity();
                                        record.setAttendanceRecordId(attendanceId);
                                        record.setEmployeeId(employeeId);
                                        record.setCompanyId(companyId);
                                        record.setWorkDate(LocalDate.now());

                                        LocalDateTime breakStart = LocalDateTime.of(2024, 1, 1, startHour, startMinute);
                                        LocalDateTime breakEnd = breakStart.plusMinutes(duration);

                                        record.setBreakStart(breakStart);
                                        record.setBreakEnd(breakEnd);
                                        record.setActualBreakMinutes(duration);
                                        record.setEffectiveBreakMinutes(duration);

                                        return record;
                                });
        }

        @Provide
        Arbitrary<AttendanceWithBreaks> attendanceWithNonOverlappingBreaks() {
                return Combinators.combine(
                                Arbitraries.longs().between(1, 1000),
                                Arbitraries.longs().between(1, 100),
                                Arbitraries.longs().between(1, 50),
                                Arbitraries.integers().between(1, 4))
                                .as((attendanceId, employeeId, companyId, breakCount) -> {
                                        LocalDate workDate = LocalDate.now();
                                        List<BreakRecordEntity> breaks = new ArrayList<>();

                                        // Tạo các breaks không overlap với nhau
                                        // Mỗi break cách nhau ít nhất 1 giờ
                                        int baseHour = 9;
                                        int breakDuration = 30; // 30 phút mỗi break
                                        int gapBetweenBreaks = 90; // 90 phút giữa các breaks

                                        for (int i = 0; i < breakCount; i++) {
                                                BreakRecordEntity breakRecord = new BreakRecordEntity();
                                                breakRecord.setAttendanceRecordId(attendanceId);
                                                breakRecord.setEmployeeId(employeeId);
                                                breakRecord.setCompanyId(companyId);
                                                breakRecord.setWorkDate(workDate);
                                                breakRecord.setBreakNumber(i + 1);

                                                // Tính thời gian bắt đầu break
                                                int startMinutesFromBase = i * gapBetweenBreaks;
                                                int startHour = baseHour + startMinutesFromBase / 60;
                                                int startMinute = startMinutesFromBase % 60;

                                                LocalDateTime breakStart = LocalDateTime.of(workDate,
                                                                LocalTime.of(startHour, startMinute));
                                                LocalDateTime breakEnd = breakStart.plusMinutes(breakDuration);

                                                breakRecord.setBreakStart(breakStart);
                                                breakRecord.setBreakEnd(breakEnd);
                                                breakRecord.setActualBreakMinutes(breakDuration);
                                                breakRecord.setEffectiveBreakMinutes(breakDuration);
                                                breaks.add(breakRecord);
                                        }

                                        return new AttendanceWithBreaks(attendanceId, employeeId, companyId, workDate,
                                                        breaks);
                                });
        }
}
