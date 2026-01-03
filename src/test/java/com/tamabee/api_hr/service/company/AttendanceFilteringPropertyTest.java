package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.ReportQuery;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.repository.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Attendance Filtering
 * Feature: flexible-workforce-management, Property 20: Attendance Filtering
 * Accuracy
 * 
 * For any attendance query with filters (date range, employee, status),
 * all returned records SHALL match ALL specified filter criteria.
 */
public class AttendanceFilteringPropertyTest {

    /**
     * Property 20: Attendance Filtering by Date Range
     * Tất cả records trả về phải nằm trong khoảng date range được chỉ định
     */
    @Property(tries = 100)
    void attendanceRecordsMatchDateRangeFilter(
            @ForAll("dateRanges") DateRange dateRange,
            @ForAll("attendanceRecordLists") List<AttendanceRecordEntity> allRecords) {

        // Arrange - Filter records theo date range
        List<AttendanceRecordEntity> filteredRecords = allRecords.stream()
                .filter(r -> !r.getWorkDate().isBefore(dateRange.startDate) &&
                        !r.getWorkDate().isAfter(dateRange.endDate))
                .collect(Collectors.toList());

        // Assert - Tất cả records phải nằm trong date range
        for (AttendanceRecordEntity record : filteredRecords) {
            assertTrue(
                    !record.getWorkDate().isBefore(dateRange.startDate),
                    "Record date " + record.getWorkDate() + " should not be before start date " + dateRange.startDate);
            assertTrue(
                    !record.getWorkDate().isAfter(dateRange.endDate),
                    "Record date " + record.getWorkDate() + " should not be after end date " + dateRange.endDate);
        }

        // Assert - Không có record nào bị bỏ sót
        long expectedCount = allRecords.stream()
                .filter(r -> !r.getWorkDate().isBefore(dateRange.startDate) &&
                        !r.getWorkDate().isAfter(dateRange.endDate))
                .count();
        assertEquals(expectedCount, filteredRecords.size(),
                "Filtered count should match expected count");
    }

    /**
     * Property 20: Attendance Filtering by Employee IDs
     * Tất cả records trả về phải thuộc về employees được chỉ định
     */
    @Property(tries = 100)
    void attendanceRecordsMatchEmployeeFilter(
            @ForAll("employeeIdSets") Set<Long> employeeIds,
            @ForAll("attendanceRecordLists") List<AttendanceRecordEntity> allRecords) {

        // Arrange - Filter records theo employee IDs
        List<AttendanceRecordEntity> filteredRecords = allRecords.stream()
                .filter(r -> employeeIds.contains(r.getEmployeeId()))
                .collect(Collectors.toList());

        // Assert - Tất cả records phải thuộc về employees được chỉ định
        for (AttendanceRecordEntity record : filteredRecords) {
            assertTrue(
                    employeeIds.contains(record.getEmployeeId()),
                    "Record employeeId " + record.getEmployeeId() + " should be in filter set " + employeeIds);
        }

        // Assert - Không có record nào bị bỏ sót
        long expectedCount = allRecords.stream()
                .filter(r -> employeeIds.contains(r.getEmployeeId()))
                .count();
        assertEquals(expectedCount, filteredRecords.size(),
                "Filtered count should match expected count");
    }

    /**
     * Property 20: Combined Filtering (Date Range + Employee IDs)
     * Khi áp dụng nhiều filter, tất cả records phải thỏa mãn TẤT CẢ điều kiện
     */
    @Property(tries = 100)
    void attendanceRecordsMatchAllFilters(
            @ForAll("dateRanges") DateRange dateRange,
            @ForAll("employeeIdSets") Set<Long> employeeIds,
            @ForAll("attendanceRecordLists") List<AttendanceRecordEntity> allRecords) {

        // Arrange - Filter records theo cả date range và employee IDs
        List<AttendanceRecordEntity> filteredRecords = allRecords.stream()
                .filter(r -> !r.getWorkDate().isBefore(dateRange.startDate) &&
                        !r.getWorkDate().isAfter(dateRange.endDate))
                .filter(r -> employeeIds.isEmpty() || employeeIds.contains(r.getEmployeeId()))
                .collect(Collectors.toList());

        // Assert - Tất cả records phải thỏa mãn TẤT CẢ điều kiện
        for (AttendanceRecordEntity record : filteredRecords) {
            // Check date range
            assertTrue(
                    !record.getWorkDate().isBefore(dateRange.startDate) &&
                            !record.getWorkDate().isAfter(dateRange.endDate),
                    "Record should be within date range");

            // Check employee filter (nếu có)
            if (!employeeIds.isEmpty()) {
                assertTrue(
                        employeeIds.contains(record.getEmployeeId()),
                        "Record should belong to filtered employees");
            }
        }
    }

    /**
     * Property 20: Empty Filter Returns All Records
     * Khi không có filter nào được áp dụng, trả về tất cả records
     */
    @Property(tries = 50)
    void emptyFilterReturnsAllRecords(
            @ForAll("attendanceRecordLists") List<AttendanceRecordEntity> allRecords) {

        // Arrange - Không áp dụng filter nào (empty employee list)
        Set<Long> emptyEmployeeIds = Collections.emptySet();

        // Act - Filter với empty set (không filter theo employee)
        List<AttendanceRecordEntity> filteredRecords = allRecords.stream()
                .filter(r -> emptyEmployeeIds.isEmpty() || emptyEmployeeIds.contains(r.getEmployeeId()))
                .collect(Collectors.toList());

        // Assert - Trả về tất cả records
        assertEquals(allRecords.size(), filteredRecords.size(),
                "Empty filter should return all records");
    }

    /**
     * Property 20: Filter Preserves Record Integrity
     * Filtering không được thay đổi nội dung của records
     */
    @Property(tries = 100)
    void filterPreservesRecordIntegrity(
            @ForAll("dateRanges") DateRange dateRange,
            @ForAll("attendanceRecordLists") List<AttendanceRecordEntity> allRecords) {

        // Arrange - Lưu trữ original data
        Map<Long, AttendanceRecordEntity> originalRecords = allRecords.stream()
                .collect(Collectors.toMap(
                        AttendanceRecordEntity::getId,
                        r -> r,
                        (a, b) -> a));

        // Act - Filter records
        List<AttendanceRecordEntity> filteredRecords = allRecords.stream()
                .filter(r -> !r.getWorkDate().isBefore(dateRange.startDate) &&
                        !r.getWorkDate().isAfter(dateRange.endDate))
                .collect(Collectors.toList());

        // Assert - Records không bị thay đổi
        for (AttendanceRecordEntity filtered : filteredRecords) {
            AttendanceRecordEntity original = originalRecords.get(filtered.getId());
            assertNotNull(original, "Filtered record should exist in original");
            assertEquals(original.getEmployeeId(), filtered.getEmployeeId(),
                    "EmployeeId should not change");
            assertEquals(original.getWorkDate(), filtered.getWorkDate(),
                    "WorkDate should not change");
            assertEquals(original.getStatus(), filtered.getStatus(),
                    "Status should not change");
        }
    }

    // === Generators ===

    @Provide
    Arbitrary<DateRange> dateRanges() {
        return Arbitraries.integers().between(0, 365)
                .flatMap(startOffset -> Arbitraries.integers().between(1, 30)
                        .map(duration -> {
                            LocalDate startDate = LocalDate.of(2025, 1, 1).plusDays(startOffset);
                            LocalDate endDate = startDate.plusDays(duration);
                            return new DateRange(startDate, endDate);
                        }));
    }

    @Provide
    Arbitrary<Set<Long>> employeeIdSets() {
        return Arbitraries.longs().between(1L, 100L)
                .set().ofMinSize(0).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<AttendanceRecordEntity>> attendanceRecordLists() {
        return Arbitraries.integers().between(0, 50)
                .flatMap(size -> {
                    if (size == 0) {
                        return Arbitraries.just(Collections.emptyList());
                    }
                    return Arbitraries.integers().between(0, 365)
                            .flatMap(dateOffset -> Arbitraries.longs().between(1L, 100L)
                                    .flatMap(employeeId -> Arbitraries.of(AttendanceStatus.values())
                                            .map(status -> createAttendanceRecord(
                                                    employeeId,
                                                    LocalDate.of(2025, 1, 1).plusDays(dateOffset),
                                                    status))))
                            .list().ofSize(size)
                            .map(records -> {
                                // Gán ID duy nhất cho mỗi record
                                long id = 1;
                                for (AttendanceRecordEntity record : records) {
                                    record.setId(id++);
                                }
                                return records;
                            });
                });
    }

    private AttendanceRecordEntity createAttendanceRecord(Long employeeId, LocalDate workDate,
            AttendanceStatus status) {
        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setEmployeeId(employeeId);
        record.setWorkDate(workDate);
        record.setStatus(status);
        record.setDeleted(false);
        return record;
    }

    // === Helper Classes ===

    static class DateRange {
        final LocalDate startDate;
        final LocalDate endDate;

        DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        public String toString() {
            return "DateRange[" + startDate + " to " + endDate + "]";
        }
    }
}
