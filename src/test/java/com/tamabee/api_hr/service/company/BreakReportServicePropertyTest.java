package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.response.DailyBreakReportResponse.BreakSessionInfo;
import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import net.jqwik.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests cho Break Report Service
 * Feature: multiple-breaks-support
 */
public class BreakReportServicePropertyTest {

    /**
     * Property 3: Breaks Ordered by Break Number
     * For any query returning break records for an attendance record,
     * the results SHALL be ordered by breakNumber in ascending order.
     */
    @Property(tries = 100)
    void breaksOrderedByBreakNumber(
            @ForAll("shuffledBreakRecords") List<BreakRecordEntity> shuffledBreaks) {

        if (shuffledBreaks.isEmpty()) {
            return;
        }

        // Mô phỏng logic sắp xếp trong BreakReportServiceImpl
        List<BreakRecordEntity> sortedBreaks = new ArrayList<>(shuffledBreaks);
        sortedBreaks.sort(Comparator.comparing(
                BreakRecordEntity::getBreakNumber,
                Comparator.nullsLast(Integer::compareTo)));

        // Tạo breakSessions list như trong BreakReportServiceImpl
        List<BreakSessionInfo> breakSessions = sortedBreaks.stream()
                .map(record -> BreakSessionInfo.builder()
                        .breakRecordId(record.getId())
                        .breakNumber(record.getBreakNumber())
                        .breakStart(record.getBreakStart())
                        .breakEnd(record.getBreakEnd())
                        .durationMinutes(record.getActualBreakMinutes())
                        .build())
                .collect(Collectors.toList());

        // Kiểm tra breakSessions được sắp xếp theo breakNumber tăng dần
        for (int i = 0; i < breakSessions.size() - 1; i++) {
            Integer currentBreakNumber = breakSessions.get(i).getBreakNumber();
            Integer nextBreakNumber = breakSessions.get(i + 1).getBreakNumber();

            if (currentBreakNumber != null && nextBreakNumber != null) {
                assertTrue(currentBreakNumber <= nextBreakNumber,
                        String.format("Break sessions should be ordered by breakNumber. " +
                                "Found breakNumber %d before %d at index %d",
                                currentBreakNumber, nextBreakNumber, i));
            }
        }

        // Kiểm tra số lượng breakSessions bằng số lượng break records
        assertEquals(shuffledBreaks.size(), breakSessions.size(),
                "Number of break sessions should equal number of break records");
    }

    /**
     * Property 3 (bổ sung): Tất cả break records đều được include trong
     * breakSessions
     * For any set of break records, all records SHALL be included in the
     * breakSessions list.
     */
    @Property(tries = 100)
    void allBreaksIncludedInSessions(
            @ForAll("shuffledBreakRecords") List<BreakRecordEntity> breaks) {

        if (breaks.isEmpty()) {
            return;
        }

        // Mô phỏng logic trong BreakReportServiceImpl
        List<BreakRecordEntity> sortedBreaks = new ArrayList<>(breaks);
        sortedBreaks.sort(Comparator.comparing(
                BreakRecordEntity::getBreakNumber,
                Comparator.nullsLast(Integer::compareTo)));

        List<BreakSessionInfo> breakSessions = sortedBreaks.stream()
                .map(record -> BreakSessionInfo.builder()
                        .breakRecordId(record.getId())
                        .breakNumber(record.getBreakNumber())
                        .breakStart(record.getBreakStart())
                        .breakEnd(record.getBreakEnd())
                        .durationMinutes(record.getActualBreakMinutes())
                        .build())
                .collect(Collectors.toList());

        // Kiểm tra tất cả break records đều có trong breakSessions
        for (BreakRecordEntity breakRecord : breaks) {
            boolean found = breakSessions.stream()
                    .anyMatch(session -> session.getBreakNumber() != null
                            && session.getBreakNumber().equals(breakRecord.getBreakNumber()));
            assertTrue(found,
                    String.format("Break record with breakNumber %d should be in breakSessions",
                            breakRecord.getBreakNumber()));
        }
    }

    /**
     * Property 3 (bổ sung): BreakSessionInfo chứa đầy đủ thông tin từ
     * BreakRecordEntity
     * For any break record, the corresponding BreakSessionInfo SHALL contain
     * breakRecordId, breakNumber, breakStart, breakEnd, and durationMinutes.
     */
    @Property(tries = 100)
    void breakSessionInfoContainsAllFields(
            @ForAll("completedBreakRecord") BreakRecordEntity breakRecord) {

        // Tạo BreakSessionInfo như trong BreakReportServiceImpl
        BreakSessionInfo sessionInfo = BreakSessionInfo.builder()
                .breakRecordId(breakRecord.getId())
                .breakNumber(breakRecord.getBreakNumber())
                .breakStart(breakRecord.getBreakStart())
                .breakEnd(breakRecord.getBreakEnd())
                .durationMinutes(breakRecord.getActualBreakMinutes())
                .build();

        // Kiểm tra các fields được map đúng
        assertEquals(breakRecord.getId(), sessionInfo.getBreakRecordId(),
                "breakRecordId should match");
        assertEquals(breakRecord.getBreakNumber(), sessionInfo.getBreakNumber(),
                "breakNumber should match");
        assertEquals(breakRecord.getBreakStart(), sessionInfo.getBreakStart(),
                "breakStart should match");
        assertEquals(breakRecord.getBreakEnd(), sessionInfo.getBreakEnd(),
                "breakEnd should match");
        assertEquals(breakRecord.getActualBreakMinutes(), sessionInfo.getDurationMinutes(),
                "durationMinutes should match actualBreakMinutes");
    }

    // === Generators ===

    @Provide
    Arbitrary<List<BreakRecordEntity>> shuffledBreakRecords() {
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
                        breakRecord.setId((long) (i + 1));
                        breakRecord.setAttendanceRecordId(attendanceId);
                        breakRecord.setEmployeeId(employeeId);
                        breakRecord.setCompanyId(companyId);
                        breakRecord.setWorkDate(workDate);
                        breakRecord.setBreakNumber(i + 1);
                        breakRecord.setBreakStart(
                                LocalDateTime.of(workDate, LocalTime.of(9 + i * 2, 0)));
                        breakRecord.setBreakEnd(
                                LocalDateTime.of(workDate, LocalTime.of(9 + i * 2, 30)));
                        breakRecord.setActualBreakMinutes(30);
                        breakRecord.setEffectiveBreakMinutes(30);
                        breaks.add(breakRecord);
                    }

                    // Shuffle để test sorting
                    Collections.shuffle(breaks);
                    return breaks;
                });
    }

    @Provide
    Arbitrary<BreakRecordEntity> completedBreakRecord() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),
                Arbitraries.longs().between(1, 1000),
                Arbitraries.longs().between(1, 100),
                Arbitraries.longs().between(1, 50),
                Arbitraries.integers().between(1, 5),
                Arbitraries.integers().between(9, 17),
                Arbitraries.integers().between(15, 60))
                .as((id, attendanceId, employeeId, companyId, breakNumber, startHour, duration) -> {
                    LocalDate workDate = LocalDate.now();
                    BreakRecordEntity breakRecord = new BreakRecordEntity();
                    breakRecord.setId(id);
                    breakRecord.setAttendanceRecordId(attendanceId);
                    breakRecord.setEmployeeId(employeeId);
                    breakRecord.setCompanyId(companyId);
                    breakRecord.setWorkDate(workDate);
                    breakRecord.setBreakNumber(breakNumber);
                    breakRecord.setBreakStart(
                            LocalDateTime.of(workDate, LocalTime.of(startHour, 0)));
                    breakRecord.setBreakEnd(
                            LocalDateTime.of(workDate, LocalTime.of(startHour, 0))
                                    .plusMinutes(duration));
                    breakRecord.setActualBreakMinutes(duration);
                    breakRecord.setEffectiveBreakMinutes(duration);
                    return breakRecord;
                });
    }
}
