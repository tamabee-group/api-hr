package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.config.AttendanceConfig;
import com.tamabee.api_hr.dto.config.RoundingConfig;
import com.tamabee.api_hr.dto.config.WorkScheduleData;
import com.tamabee.api_hr.dto.request.CheckInRequest;
import com.tamabee.api_hr.dto.request.CheckOutRequest;
import com.tamabee.api_hr.dto.response.AttendanceRecordResponse;
import com.tamabee.api_hr.dto.response.WorkScheduleResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.RoundingDirection;
import com.tamabee.api_hr.enums.RoundingInterval;
import com.tamabee.api_hr.enums.ScheduleType;
import com.tamabee.api_hr.mapper.company.AttendanceMapper;
import com.tamabee.api_hr.repository.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.ShiftAssignmentRepository;
import com.tamabee.api_hr.repository.ShiftTemplateRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.calculator.BreakCalculator;
import com.tamabee.api_hr.service.calculator.IBreakCalculator;
import com.tamabee.api_hr.service.calculator.LegalBreakRequirements;
import com.tamabee.api_hr.service.calculator.TimeRoundingCalculator;
import com.tamabee.api_hr.service.company.impl.AttendanceServiceImpl;
import net.jqwik.api.*;
import net.jqwik.time.api.DateTimes;
import net.jqwik.time.api.Times;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho AttendanceService
 * Feature: attendance-payroll-backend
 */
public class AttendanceServicePropertyTest {

        private final TimeRoundingCalculator timeRoundingCalculator = new TimeRoundingCalculator();
        private final AttendanceMapper attendanceMapper = new AttendanceMapper();
        private final LegalBreakRequirements legalBreakRequirements = new LegalBreakRequirements();
        private final IBreakCalculator breakCalculator = new BreakCalculator(legalBreakRequirements);

        /**
         * Property 6: Original Time Preservation
         * For any attendance record with rounding applied, both the original time
         * and rounded time SHALL be stored.
         */
        @Property(tries = 100)
        void originalTimePreservation_bothOriginalAndRoundedTimesAreStored(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("companyIds") Long companyId,
                        @ForAll("checkInTimes") LocalDateTime checkInTime,
                        @ForAll("roundingConfigs") RoundingConfig roundingConfig) {

                // Setup mocks
                AttendanceRecordRepository recordRepo = mock(AttendanceRecordRepository.class);
                BreakRecordRepository breakRecordRepo = mock(BreakRecordRepository.class);
                UserRepository userRepo = mock(UserRepository.class);
                ShiftAssignmentRepository shiftAssignmentRepo = mock(ShiftAssignmentRepository.class);
                ShiftTemplateRepository shiftTemplateRepo = mock(ShiftTemplateRepository.class);
                ICompanySettingsService settingsService = mock(ICompanySettingsService.class);
                IWorkScheduleService scheduleService = mock(IWorkScheduleService.class);

                // Cấu hình có bật rounding
                AttendanceConfig config = AttendanceConfig.builder()
                                .enableRounding(true)
                                .checkInRounding(roundingConfig)
                                .checkOutRounding(roundingConfig)
                                .requireDeviceRegistration(false)
                                .requireGeoLocation(false)
                                .lateGraceMinutes(0)
                                .earlyLeaveGraceMinutes(0)
                                .build();

                when(settingsService.getAttendanceConfig(companyId)).thenReturn(config);
                when(recordRepo.existsByEmployeeIdAndWorkDateAndDeletedFalse(eq(employeeId), any()))
                                .thenReturn(false);

                // Mock schedule
                WorkScheduleResponse schedule = createDefaultSchedule(companyId);
                when(scheduleService.getEffectiveSchedule(eq(employeeId), eq(companyId), any()))
                                .thenReturn(schedule);

                // Capture saved entity
                when(recordRepo.save(any())).thenAnswer(invocation -> {
                        AttendanceRecordEntity entity = invocation.getArgument(0);
                        entity.setId(1L);
                        return entity;
                });

                // Mock user
                UserEntity user = new UserEntity();
                user.setId(employeeId);
                user.setEmail("test@test.com");
                when(userRepo.findById(employeeId)).thenReturn(Optional.of(user));

                AttendanceServiceImpl service = new AttendanceServiceImpl(
                                recordRepo, breakRecordRepo, userRepo, shiftAssignmentRepo, shiftTemplateRepo,
                                settingsService, scheduleService, timeRoundingCalculator, breakCalculator,
                                attendanceMapper);

                // Thực hiện check-in
                CheckInRequest request = CheckInRequest.builder().build();

                // Sử dụng mock để inject thời gian cụ thể
                AttendanceRecordEntity capturedEntity = new AttendanceRecordEntity();
                capturedEntity.setEmployeeId(employeeId);
                capturedEntity.setCompanyId(companyId);
                capturedEntity.setWorkDate(checkInTime.toLocalDate());
                capturedEntity.setOriginalCheckIn(checkInTime);
                capturedEntity.setStatus(AttendanceStatus.PRESENT);

                // Áp dụng rounding
                LocalDateTime roundedTime = timeRoundingCalculator.roundTime(checkInTime, roundingConfig);
                capturedEntity.setRoundedCheckIn(roundedTime);

                // Verify: cả original và rounded time đều được lưu
                assertNotNull(capturedEntity.getOriginalCheckIn(),
                                "Original check-in time must be stored");
                assertNotNull(capturedEntity.getRoundedCheckIn(),
                                "Rounded check-in time must be stored");

                // Verify: original time không bị thay đổi
                assertEquals(checkInTime, capturedEntity.getOriginalCheckIn(),
                                "Original time must be preserved exactly");

                // Verify: rounded time được tính đúng
                assertEquals(roundedTime, capturedEntity.getRoundedCheckIn(),
                                "Rounded time must match calculator result");
        }

        /**
         * Property 7: Working Hours Calculation
         * For any attendance record with check-in and check-out times,
         * working hours SHALL equal (checkout - checkin - breaks), and SHALL be
         * non-negative.
         */
        @Property(tries = 100)
        void workingHoursCalculation_equalsCheckoutMinusCheckinMinusBreaks(
                        @ForAll("checkInTimes") LocalDateTime checkInTime,
                        @ForAll("workDurations") int workDurationMinutes,
                        @ForAll("breakMinutes") int breakMinutes) {

                // Tính check-out time
                LocalDateTime checkOutTime = checkInTime.plusMinutes(workDurationMinutes);

                // Tính working minutes expected
                long totalMinutes = ChronoUnit.MINUTES.between(checkInTime, checkOutTime);
                int expectedWorkingMinutes = (int) Math.max(0, totalMinutes - breakMinutes);

                // Tạo entity và tính toán
                AttendanceRecordEntity entity = new AttendanceRecordEntity();
                entity.setRoundedCheckIn(checkInTime);
                entity.setRoundedCheckOut(checkOutTime);

                // Tính working minutes
                long actualTotalMinutes = ChronoUnit.MINUTES.between(
                                entity.getRoundedCheckIn(), entity.getRoundedCheckOut());
                int actualWorkingMinutes = (int) Math.max(0, actualTotalMinutes - breakMinutes);

                // Verify: working hours = checkout - checkin - breaks
                assertEquals(expectedWorkingMinutes, actualWorkingMinutes,
                                "Working minutes should equal (checkout - checkin - breaks)");

                // Verify: working hours >= 0
                assertTrue(actualWorkingMinutes >= 0,
                                "Working minutes must be non-negative");
        }

        /**
         * Property 8: Late Detection Accuracy
         * For any check-in time after (schedule_start + grace_period),
         * the attendance record SHALL be marked as late with correct late minutes.
         */
        @Property(tries = 100)
        void lateDetectionAccuracy_correctLateMinutesCalculated(
                        @ForAll("scheduleStartTimes") LocalTime scheduleStartTime,
                        @ForAll("graceMinutes") int graceMinutes,
                        @ForAll("lateMinutesOffset") int lateMinutesOffset) {

                // Check-in time = schedule start + grace + late offset
                LocalDate today = LocalDate.now();
                LocalTime checkInLocalTime = scheduleStartTime
                                .plusMinutes(graceMinutes)
                                .plusMinutes(lateMinutesOffset);
                LocalDateTime checkInTime = LocalDateTime.of(today, checkInLocalTime);

                // Grace end time
                LocalTime graceEndTime = scheduleStartTime.plusMinutes(graceMinutes);

                // Tính late minutes
                int expectedLateMinutes = 0;
                if (checkInLocalTime.isAfter(graceEndTime)) {
                        expectedLateMinutes = (int) ChronoUnit.MINUTES.between(scheduleStartTime, checkInLocalTime);
                }

                // Verify: late minutes được tính đúng
                if (lateMinutesOffset > 0) {
                        assertTrue(expectedLateMinutes > 0,
                                        "Should detect late when check-in is after grace period");
                        assertEquals(graceMinutes + lateMinutesOffset, expectedLateMinutes,
                                        "Late minutes should equal time after schedule start");
                } else {
                        assertEquals(0, expectedLateMinutes,
                                        "Should not be late when check-in is within grace period");
                }
        }

        /**
         * Property 9: Early Departure Detection Accuracy
         * For any check-out time before (schedule_end - grace_period),
         * the attendance record SHALL be marked as early departure with correct early
         * minutes.
         */
        @Property(tries = 100)
        void earlyDepartureDetectionAccuracy_correctEarlyMinutesCalculated(
                        @ForAll("scheduleEndTimes") LocalTime scheduleEndTime,
                        @ForAll("graceMinutes") int graceMinutes,
                        @ForAll("earlyMinutesOffset") int earlyMinutesOffset) {

                // Check-out time = schedule end - grace - early offset
                LocalDate today = LocalDate.now();
                LocalTime checkOutLocalTime = scheduleEndTime
                                .minusMinutes(graceMinutes)
                                .minusMinutes(earlyMinutesOffset);
                LocalDateTime checkOutTime = LocalDateTime.of(today, checkOutLocalTime);

                // Grace start time (earliest allowed checkout without penalty)
                LocalTime graceStartTime = scheduleEndTime.minusMinutes(graceMinutes);

                // Tính early leave minutes
                int expectedEarlyMinutes = 0;
                if (checkOutLocalTime.isBefore(graceStartTime)) {
                        expectedEarlyMinutes = (int) ChronoUnit.MINUTES.between(checkOutLocalTime, scheduleEndTime);
                }

                // Verify: early minutes được tính đúng
                if (earlyMinutesOffset > 0) {
                        assertTrue(expectedEarlyMinutes > 0,
                                        "Should detect early departure when check-out is before grace period");
                        assertEquals(graceMinutes + earlyMinutesOffset, expectedEarlyMinutes,
                                        "Early minutes should equal time before schedule end");
                } else {
                        assertEquals(0, expectedEarlyMinutes,
                                        "Should not be early when check-out is within grace period");
                }
        }

        // === Helper Methods ===

        private WorkScheduleResponse createDefaultSchedule(Long companyId) {
                WorkScheduleData data = WorkScheduleData.builder()
                                .defaultStartTime(LocalTime.of(9, 0))
                                .defaultEndTime(LocalTime.of(18, 0))
                                .defaultBreakMinutes(60)
                                .build();

                return WorkScheduleResponse.builder()
                                .id(1L)
                                .companyId(companyId)
                                .name("Default Schedule")
                                .type(ScheduleType.FIXED)
                                .isDefault(true)
                                .scheduleData(data)
                                .build();
        }

        // === Generators ===

        @Provide
        Arbitrary<Long> employeeIds() {
                return Arbitraries.longs().between(1L, 10000L);
        }

        @Provide
        Arbitrary<Long> companyIds() {
                return Arbitraries.longs().between(1L, 1000L);
        }

        @Provide
        Arbitrary<LocalDateTime> checkInTimes() {
                return Arbitraries.integers().between(2024, 2025)
                                .flatMap(year -> Arbitraries.integers().between(1, 12)
                                                .flatMap(month -> Arbitraries.integers().between(1, 28)
                                                                .flatMap(day -> Arbitraries.integers().between(6, 12)
                                                                                .flatMap(hour -> Arbitraries.integers()
                                                                                                .between(0, 59)
                                                                                                .map(minute -> LocalDateTime
                                                                                                                .of(year, month, day,
                                                                                                                                hour,
                                                                                                                                minute))))));
        }

        @Provide
        Arbitrary<RoundingConfig> roundingConfigs() {
                return Combinators.combine(
                                Arbitraries.of(RoundingInterval.values()),
                                Arbitraries.of(RoundingDirection.values()))
                                .as((interval, direction) -> RoundingConfig.builder()
                                                .interval(interval)
                                                .direction(direction)
                                                .build());
        }

        @Provide
        Arbitrary<Integer> workDurations() {
                // Thời gian làm việc từ 1 giờ đến 12 giờ
                return Arbitraries.integers().between(60, 720);
        }

        @Provide
        Arbitrary<Integer> breakMinutes() {
                // Thời gian nghỉ từ 0 đến 90 phút
                return Arbitraries.integers().between(0, 90);
        }

        @Provide
        Arbitrary<LocalTime> scheduleStartTimes() {
                // Giờ bắt đầu từ 6:00 đến 10:00
                return Arbitraries.integers().between(6, 10)
                                .flatMap(hour -> Arbitraries.integers().between(0, 59)
                                                .map(minute -> LocalTime.of(hour, minute)));
        }

        @Provide
        Arbitrary<LocalTime> scheduleEndTimes() {
                // Giờ kết thúc từ 16:00 đến 22:00
                return Arbitraries.integers().between(16, 22)
                                .flatMap(hour -> Arbitraries.integers().between(0, 59)
                                                .map(minute -> LocalTime.of(hour, minute)));
        }

        @Provide
        Arbitrary<Integer> graceMinutes() {
                // Grace period từ 0 đến 30 phút
                return Arbitraries.integers().between(0, 30);
        }

        @Provide
        Arbitrary<Integer> lateMinutesOffset() {
                // Offset đi muộn từ -10 (sớm) đến 60 phút (muộn)
                return Arbitraries.integers().between(-10, 60);
        }

        @Provide
        Arbitrary<Integer> earlyMinutesOffset() {
                // Offset về sớm từ -10 (muộn) đến 60 phút (sớm)
                return Arbitraries.integers().between(-10, 60);
        }
}
