package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.AdjustAttendanceRequest;
import com.tamabee.api_hr.dto.request.CreateAdjustmentRequest;
import com.tamabee.api_hr.dto.response.AdjustmentRequestResponse;
import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.AdjustmentStatus;
import com.tamabee.api_hr.enums.AttendanceStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.mapper.company.AttendanceAdjustmentMapper;
import com.tamabee.api_hr.repository.AttendanceAdjustmentRequestRepository;
import com.tamabee.api_hr.repository.AttendanceRecordRepository;
import com.tamabee.api_hr.repository.BreakRecordRepository;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.impl.AttendanceAdjustmentServiceImpl;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho AttendanceAdjustmentService
 * Feature: attendance-payroll-backend
 */
public class AttendanceAdjustmentServicePropertyTest {

        private final AttendanceAdjustmentMapper adjustmentMapper = new AttendanceAdjustmentMapper();

        /**
         * Property 21: Adjustment Request Workflow
         * For any approved adjustment request, the attendance record SHALL be updated
         * with the requested times and all derived values (working hours, overtime,
         * late/early status) SHALL be recalculated.
         */
        @Property(tries = 100)
        void adjustmentRequestWorkflow_approvedRequestUpdatesAttendanceRecord(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("companyIds") Long companyId,
                        @ForAll("managerIds") Long managerId,
                        @ForAll("attendanceRecordIds") Long attendanceRecordId,
                        @ForAll("checkInTimes") LocalDateTime originalCheckIn,
                        @ForAll("checkOutTimes") LocalDateTime originalCheckOut,
                        @ForAll("adjustmentOffsets") int checkInOffsetMinutes,
                        @ForAll("adjustmentOffsets") int checkOutOffsetMinutes) {

                // Setup mocks
                AttendanceAdjustmentRequestRepository adjustmentRepo = mock(
                                AttendanceAdjustmentRequestRepository.class);
                AttendanceRecordRepository recordRepo = mock(AttendanceRecordRepository.class);
                BreakRecordRepository breakRecordRepo = mock(BreakRecordRepository.class);
                UserRepository userRepo = mock(UserRepository.class);
                IAttendanceService attendanceService = mock(IAttendanceService.class);

                // Tạo attendance record gốc
                AttendanceRecordEntity attendanceRecord = new AttendanceRecordEntity();
                attendanceRecord.setId(attendanceRecordId);
                attendanceRecord.setEmployeeId(employeeId);
                attendanceRecord.setCompanyId(companyId);
                attendanceRecord.setWorkDate(originalCheckIn.toLocalDate());
                attendanceRecord.setOriginalCheckIn(originalCheckIn);
                attendanceRecord.setOriginalCheckOut(originalCheckOut);
                attendanceRecord.setStatus(AttendanceStatus.PRESENT);

                when(recordRepo.findByIdAndDeletedFalse(attendanceRecordId))
                                .thenReturn(Optional.of(attendanceRecord));

                // Tạo adjustment request đang pending
                LocalDateTime requestedCheckIn = originalCheckIn.plusMinutes(checkInOffsetMinutes);
                LocalDateTime requestedCheckOut = originalCheckOut.plusMinutes(checkOutOffsetMinutes);

                AttendanceAdjustmentRequestEntity adjustmentRequest = new AttendanceAdjustmentRequestEntity();
                adjustmentRequest.setId(1L);
                adjustmentRequest.setEmployeeId(employeeId);
                adjustmentRequest.setCompanyId(companyId);
                adjustmentRequest.setAttendanceRecordId(attendanceRecordId);
                adjustmentRequest.setOriginalCheckIn(originalCheckIn);
                adjustmentRequest.setOriginalCheckOut(originalCheckOut);
                adjustmentRequest.setRequestedCheckIn(requestedCheckIn);
                adjustmentRequest.setRequestedCheckOut(requestedCheckOut);
                adjustmentRequest.setReason("Test adjustment");
                adjustmentRequest.setStatus(AdjustmentStatus.PENDING);

                when(adjustmentRepo.findByIdAndDeletedFalse(1L))
                                .thenReturn(Optional.of(adjustmentRequest));

                // Mock save để capture entity
                when(adjustmentRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                // Mock user
                UserEntity user = new UserEntity();
                user.setId(employeeId);
                user.setEmail("employee@test.com");
                when(userRepo.findById(employeeId)).thenReturn(Optional.of(user));

                UserEntity manager = new UserEntity();
                manager.setId(managerId);
                manager.setEmail("manager@test.com");
                when(userRepo.findById(managerId)).thenReturn(Optional.of(manager));

                // Capture adjustAttendance call
                final AdjustAttendanceRequest[] capturedRequest = new AdjustAttendanceRequest[1];
                doAnswer(invocation -> {
                        capturedRequest[0] = invocation.getArgument(2);
                        return null;
                }).when(attendanceService).adjustAttendance(eq(attendanceRecordId), eq(managerId), any());

                AttendanceAdjustmentServiceImpl service = new AttendanceAdjustmentServiceImpl(
                                adjustmentRepo, recordRepo, breakRecordRepo, userRepo, attendanceService,
                                adjustmentMapper);

                // Thực hiện approve
                AdjustmentRequestResponse response = service.approveAdjustment(1L, managerId, "Approved");

                // Verify: trạng thái được cập nhật thành APPROVED
                assertEquals(AdjustmentStatus.APPROVED, response.getStatus(),
                                "Status should be APPROVED after approval");

                // Verify: approvedBy được set
                assertEquals(managerId, response.getApprovedBy(),
                                "ApprovedBy should be set to manager ID");

                // Verify: approvedAt được set
                assertNotNull(response.getApprovedAt(),
                                "ApprovedAt should be set");

                // Verify: attendanceService.adjustAttendance được gọi
                verify(attendanceService).adjustAttendance(eq(attendanceRecordId), eq(managerId), any());

                // Verify: request được truyền đúng thời gian
                assertNotNull(capturedRequest[0], "AdjustAttendanceRequest should be captured");
                assertEquals(requestedCheckIn, capturedRequest[0].getCheckInTime(),
                                "Requested check-in time should be passed to attendance service");
                assertEquals(requestedCheckOut, capturedRequest[0].getCheckOutTime(),
                                "Requested check-out time should be passed to attendance service");
        }

        /**
         * Property 22: Adjustment Request History
         * For any adjustment request (approved or rejected), the original times,
         * requested times, and decision details SHALL be preserved for audit.
         */
        @Property(tries = 100)
        void adjustmentRequestHistory_preservesAllTimesAndDecisionDetails(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("companyIds") Long companyId,
                        @ForAll("managerIds") Long managerId,
                        @ForAll("attendanceRecordIds") Long attendanceRecordId,
                        @ForAll("checkInTimes") LocalDateTime originalCheckIn,
                        @ForAll("checkOutTimes") LocalDateTime originalCheckOut,
                        @ForAll("adjustmentOffsets") int checkInOffsetMinutes,
                        @ForAll("reasons") String reason,
                        @ForAll("approvalDecisions") boolean isApproved) {

                // Setup mocks
                AttendanceAdjustmentRequestRepository adjustmentRepo = mock(
                                AttendanceAdjustmentRequestRepository.class);
                AttendanceRecordRepository recordRepo = mock(AttendanceRecordRepository.class);
                BreakRecordRepository breakRecordRepo = mock(BreakRecordRepository.class);
                UserRepository userRepo = mock(UserRepository.class);
                IAttendanceService attendanceService = mock(IAttendanceService.class);

                // Tạo attendance record
                AttendanceRecordEntity attendanceRecord = new AttendanceRecordEntity();
                attendanceRecord.setId(attendanceRecordId);
                attendanceRecord.setEmployeeId(employeeId);
                attendanceRecord.setCompanyId(companyId);
                attendanceRecord.setWorkDate(originalCheckIn.toLocalDate());
                attendanceRecord.setOriginalCheckIn(originalCheckIn);
                attendanceRecord.setOriginalCheckOut(originalCheckOut);
                attendanceRecord.setStatus(AttendanceStatus.PRESENT);

                when(recordRepo.findByIdAndDeletedFalse(attendanceRecordId))
                                .thenReturn(Optional.of(attendanceRecord));

                // Tạo adjustment request
                LocalDateTime requestedCheckIn = originalCheckIn.plusMinutes(checkInOffsetMinutes);

                AttendanceAdjustmentRequestEntity adjustmentRequest = new AttendanceAdjustmentRequestEntity();
                adjustmentRequest.setId(1L);
                adjustmentRequest.setEmployeeId(employeeId);
                adjustmentRequest.setCompanyId(companyId);
                adjustmentRequest.setAttendanceRecordId(attendanceRecordId);
                adjustmentRequest.setOriginalCheckIn(originalCheckIn);
                adjustmentRequest.setOriginalCheckOut(originalCheckOut);
                adjustmentRequest.setRequestedCheckIn(requestedCheckIn);
                adjustmentRequest.setRequestedCheckOut(null);
                adjustmentRequest.setReason(reason);
                adjustmentRequest.setStatus(AdjustmentStatus.PENDING);

                when(adjustmentRepo.findByIdAndDeletedFalse(1L))
                                .thenReturn(Optional.of(adjustmentRequest));

                // Capture saved entity
                final AttendanceAdjustmentRequestEntity[] savedEntity = new AttendanceAdjustmentRequestEntity[1];
                when(adjustmentRepo.save(any())).thenAnswer(invocation -> {
                        savedEntity[0] = invocation.getArgument(0);
                        return savedEntity[0];
                });

                // Mock users
                UserEntity user = new UserEntity();
                user.setId(employeeId);
                user.setEmail("employee@test.com");
                when(userRepo.findById(employeeId)).thenReturn(Optional.of(user));

                UserEntity manager = new UserEntity();
                manager.setId(managerId);
                manager.setEmail("manager@test.com");
                when(userRepo.findById(managerId)).thenReturn(Optional.of(manager));

                AttendanceAdjustmentServiceImpl service = new AttendanceAdjustmentServiceImpl(
                                adjustmentRepo, recordRepo, breakRecordRepo, userRepo, attendanceService,
                                adjustmentMapper);

                // Thực hiện approve hoặc reject
                AdjustmentRequestResponse response;
                if (isApproved) {
                        response = service.approveAdjustment(1L, managerId, "Approved comment");
                } else {
                        response = service.rejectAdjustment(1L, managerId, "Rejection reason");
                }

                // Verify: original times được bảo toàn
                assertEquals(originalCheckIn, response.getOriginalCheckIn(),
                                "Original check-in time must be preserved");
                assertEquals(originalCheckOut, response.getOriginalCheckOut(),
                                "Original check-out time must be preserved");

                // Verify: requested times được bảo toàn
                assertEquals(requestedCheckIn, response.getRequestedCheckIn(),
                                "Requested check-in time must be preserved");

                // Verify: reason được bảo toàn
                assertEquals(reason, response.getReason(),
                                "Original reason must be preserved");

                // Verify: decision details được lưu
                assertNotNull(response.getApprovedBy(),
                                "ApprovedBy (decision maker) must be recorded");
                assertNotNull(response.getApprovedAt(),
                                "ApprovedAt (decision time) must be recorded");

                if (isApproved) {
                        assertEquals(AdjustmentStatus.APPROVED, response.getStatus(),
                                        "Status should be APPROVED");
                        assertEquals("Approved comment", response.getApproverComment(),
                                        "Approver comment should be preserved");
                } else {
                        assertEquals(AdjustmentStatus.REJECTED, response.getStatus(),
                                        "Status should be REJECTED");
                        assertEquals("Rejection reason", response.getRejectionReason(),
                                        "Rejection reason should be preserved");
                }

                // Verify: entity được lưu với đầy đủ thông tin
                assertNotNull(savedEntity[0], "Entity should be saved");
                assertEquals(originalCheckIn, savedEntity[0].getOriginalCheckIn(),
                                "Saved entity must preserve original check-in");
                assertEquals(originalCheckOut, savedEntity[0].getOriginalCheckOut(),
                                "Saved entity must preserve original check-out");
                assertEquals(requestedCheckIn, savedEntity[0].getRequestedCheckIn(),
                                "Saved entity must preserve requested check-in");
        }

        /**
         * Property 5: Break Record ID Required for Break Adjustment
         * For any adjustment request that includes break time fields
         * (requestedBreakStart
         * or requestedBreakEnd) but has null breakRecordId, the system SHALL reject the
         * request with error code BREAK_RECORD_ID_REQUIRED.
         */
        @Property(tries = 100)
        void breakRecordIdRequired_rejectsRequestWithBreakFieldsButNoBreakRecordId(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("companyIds") Long companyId,
                        @ForAll("attendanceRecordIds") Long attendanceRecordId,
                        @ForAll("checkInTimes") LocalDateTime checkIn,
                        @ForAll("checkOutTimes") LocalDateTime checkOut,
                        @ForAll("breakStartTimes") LocalDateTime breakStart,
                        @ForAll("breakEndTimes") LocalDateTime breakEnd,
                        @ForAll("breakFieldCombinations") int breakFieldCombination,
                        @ForAll("reasons") String reason) {

                // Setup mocks
                AttendanceAdjustmentRequestRepository adjustmentRepo = mock(
                                AttendanceAdjustmentRequestRepository.class);
                AttendanceRecordRepository recordRepo = mock(AttendanceRecordRepository.class);
                BreakRecordRepository breakRecordRepo = mock(BreakRecordRepository.class);
                UserRepository userRepo = mock(UserRepository.class);
                IAttendanceService attendanceService = mock(IAttendanceService.class);

                // Tạo attendance record
                AttendanceRecordEntity attendanceRecord = new AttendanceRecordEntity();
                attendanceRecord.setId(attendanceRecordId);
                attendanceRecord.setEmployeeId(employeeId);
                attendanceRecord.setCompanyId(companyId);
                attendanceRecord.setWorkDate(checkIn.toLocalDate());
                attendanceRecord.setOriginalCheckIn(checkIn);
                attendanceRecord.setOriginalCheckOut(checkOut);
                attendanceRecord.setStatus(AttendanceStatus.PRESENT);

                when(recordRepo.findByIdAndDeletedFalse(attendanceRecordId))
                                .thenReturn(Optional.of(attendanceRecord));

                // Không có pending adjustment
                when(adjustmentRepo.existsPendingByAttendanceRecordId(attendanceRecordId))
                                .thenReturn(false);

                AttendanceAdjustmentServiceImpl service = new AttendanceAdjustmentServiceImpl(
                                adjustmentRepo, recordRepo, breakRecordRepo, userRepo, attendanceService,
                                adjustmentMapper);

                // Tạo request với break fields nhưng không có breakRecordId
                CreateAdjustmentRequest request = CreateAdjustmentRequest.builder()
                                .attendanceRecordId(attendanceRecordId)
                                .breakRecordId(null) // Không có breakRecordId
                                .reason(reason)
                                .build();

                // Set break fields dựa trên combination
                // 1 = chỉ breakStart, 2 = chỉ breakEnd, 3 = cả hai
                switch (breakFieldCombination) {
                        case 1:
                                request.setRequestedBreakStart(breakStart);
                                break;
                        case 2:
                                request.setRequestedBreakEnd(breakEnd);
                                break;
                        case 3:
                                request.setRequestedBreakStart(breakStart);
                                request.setRequestedBreakEnd(breakEnd);
                                break;
                }

                // Thực hiện và verify exception
                BadRequestException exception = assertThrows(
                                BadRequestException.class,
                                () -> service.createAdjustmentRequest(employeeId, companyId, request),
                                "Should throw BadRequestException when break fields provided without breakRecordId");

                assertEquals(ErrorCode.BREAK_RECORD_ID_REQUIRED.getCode(), exception.getErrorCode(),
                                "Error code should be BREAK_RECORD_ID_REQUIRED");
        }

        /**
         * Property 4: Specific Break Adjustment
         * For any adjustment request with a valid breakRecordId, when approved, only
         * that specific break record SHALL be updated, and all other break records
         * SHALL remain unchanged.
         */
        @Property(tries = 100)
        void specificBreakAdjustment_onlyUpdatesSpecifiedBreakRecord(
                        @ForAll("employeeIds") Long employeeId,
                        @ForAll("companyIds") Long companyId,
                        @ForAll("managerIds") Long managerId,
                        @ForAll("attendanceRecordIds") Long attendanceRecordId,
                        @ForAll("breakRecordIds") Long targetBreakRecordId,
                        @ForAll("checkInTimes") LocalDateTime checkIn,
                        @ForAll("checkOutTimes") LocalDateTime checkOut,
                        @ForAll("breakStartTimes") LocalDateTime originalBreakStart,
                        @ForAll("breakEndTimes") LocalDateTime originalBreakEnd,
                        @ForAll("adjustmentOffsets") int breakStartOffset,
                        @ForAll("adjustmentOffsets") int breakEndOffset,
                        @ForAll("reasons") String reason) {

                // Setup mocks
                AttendanceAdjustmentRequestRepository adjustmentRepo = mock(
                                AttendanceAdjustmentRequestRepository.class);
                AttendanceRecordRepository recordRepo = mock(AttendanceRecordRepository.class);
                BreakRecordRepository breakRecordRepo = mock(BreakRecordRepository.class);
                UserRepository userRepo = mock(UserRepository.class);
                IAttendanceService attendanceService = mock(IAttendanceService.class);

                // Tạo attendance record
                AttendanceRecordEntity attendanceRecord = new AttendanceRecordEntity();
                attendanceRecord.setId(attendanceRecordId);
                attendanceRecord.setEmployeeId(employeeId);
                attendanceRecord.setCompanyId(companyId);
                attendanceRecord.setWorkDate(checkIn.toLocalDate());
                attendanceRecord.setOriginalCheckIn(checkIn);
                attendanceRecord.setOriginalCheckOut(checkOut);
                attendanceRecord.setStatus(AttendanceStatus.PRESENT);

                when(recordRepo.findByIdAndDeletedFalse(attendanceRecordId))
                                .thenReturn(Optional.of(attendanceRecord));

                // Tạo adjustment request với breakRecordId cụ thể
                LocalDateTime requestedBreakStart = originalBreakStart.plusMinutes(breakStartOffset);
                LocalDateTime requestedBreakEnd = originalBreakEnd.plusMinutes(breakEndOffset);

                AttendanceAdjustmentRequestEntity adjustmentRequest = new AttendanceAdjustmentRequestEntity();
                adjustmentRequest.setId(1L);
                adjustmentRequest.setEmployeeId(employeeId);
                adjustmentRequest.setCompanyId(companyId);
                adjustmentRequest.setAttendanceRecordId(attendanceRecordId);
                adjustmentRequest.setBreakRecordId(targetBreakRecordId);
                adjustmentRequest.setOriginalCheckIn(checkIn);
                adjustmentRequest.setOriginalCheckOut(checkOut);
                adjustmentRequest.setOriginalBreakStart(originalBreakStart);
                adjustmentRequest.setOriginalBreakEnd(originalBreakEnd);
                adjustmentRequest.setRequestedBreakStart(requestedBreakStart);
                adjustmentRequest.setRequestedBreakEnd(requestedBreakEnd);
                adjustmentRequest.setReason(reason);
                adjustmentRequest.setStatus(AdjustmentStatus.PENDING);

                when(adjustmentRepo.findByIdAndDeletedFalse(1L))
                                .thenReturn(Optional.of(adjustmentRequest));

                // Mock save
                when(adjustmentRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

                // Mock users
                UserEntity user = new UserEntity();
                user.setId(employeeId);
                user.setEmail("employee@test.com");
                when(userRepo.findById(employeeId)).thenReturn(Optional.of(user));

                UserEntity manager = new UserEntity();
                manager.setId(managerId);
                manager.setEmail("manager@test.com");
                when(userRepo.findById(managerId)).thenReturn(Optional.of(manager));

                // Capture adjustAttendance call để verify breakRecordId được truyền đúng
                final AdjustAttendanceRequest[] capturedRequest = new AdjustAttendanceRequest[1];
                doAnswer(invocation -> {
                        capturedRequest[0] = invocation.getArgument(2);
                        return null;
                }).when(attendanceService).adjustAttendance(eq(attendanceRecordId), eq(managerId), any());

                AttendanceAdjustmentServiceImpl service = new AttendanceAdjustmentServiceImpl(
                                adjustmentRepo, recordRepo, breakRecordRepo, userRepo, attendanceService,
                                adjustmentMapper);

                // Thực hiện approve
                AdjustmentRequestResponse response = service.approveAdjustment(1L, managerId, "Approved");

                // Verify: trạng thái được cập nhật thành APPROVED
                assertEquals(AdjustmentStatus.APPROVED, response.getStatus(),
                                "Status should be APPROVED after approval");

                // Verify: attendanceService.adjustAttendance được gọi
                verify(attendanceService).adjustAttendance(eq(attendanceRecordId), eq(managerId), any());

                // Verify: breakAdjustments được truyền đúng trong request
                assertNotNull(capturedRequest[0], "AdjustAttendanceRequest should be captured");
                assertNotNull(capturedRequest[0].getBreakAdjustments(), "BreakAdjustments should not be null");
                assertFalse(capturedRequest[0].getBreakAdjustments().isEmpty(), "BreakAdjustments should not be empty");

                // Tìm break adjustment với targetBreakRecordId
                var breakAdjustment = capturedRequest[0].getBreakAdjustments().stream()
                                .filter(ba -> targetBreakRecordId.equals(ba.getBreakRecordId()))
                                .findFirst()
                                .orElse(null);
                assertNotNull(breakAdjustment, "BreakAdjustment with targetBreakRecordId should exist");

                // Verify: break times được truyền đúng
                assertEquals(requestedBreakStart, breakAdjustment.getBreakStartTime(),
                                "Requested break start time should be passed to attendance service");
                assertEquals(requestedBreakEnd, breakAdjustment.getBreakEndTime(),
                                "Requested break end time should be passed to attendance service");
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
        Arbitrary<Long> managerIds() {
                return Arbitraries.longs().between(10001L, 20000L);
        }

        @Provide
        Arbitrary<Long> attendanceRecordIds() {
                return Arbitraries.longs().between(1L, 100000L);
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
        Arbitrary<LocalDateTime> checkOutTimes() {
                return Arbitraries.integers().between(2024, 2025)
                                .flatMap(year -> Arbitraries.integers().between(1, 12)
                                                .flatMap(month -> Arbitraries.integers().between(1, 28)
                                                                .flatMap(day -> Arbitraries.integers().between(16, 22)
                                                                                .flatMap(hour -> Arbitraries.integers()
                                                                                                .between(0, 59)
                                                                                                .map(minute -> LocalDateTime
                                                                                                                .of(year, month, day,
                                                                                                                                hour,
                                                                                                                                minute))))));
        }

        @Provide
        Arbitrary<Integer> adjustmentOffsets() {
                // Offset điều chỉnh từ -60 đến +60 phút
                return Arbitraries.integers().between(-60, 60);
        }

        @Provide
        Arbitrary<String> reasons() {
                return Arbitraries.strings()
                                .alpha()
                                .ofMinLength(10)
                                .ofMaxLength(100)
                                .map(s -> "Lý do: " + s);
        }

        @Provide
        Arbitrary<Boolean> approvalDecisions() {
                return Arbitraries.of(true, false);
        }

        @Provide
        Arbitrary<LocalDateTime> breakStartTimes() {
                return Arbitraries.integers().between(2024, 2025)
                                .flatMap(year -> Arbitraries.integers().between(1, 12)
                                                .flatMap(month -> Arbitraries.integers().between(1, 28)
                                                                .flatMap(day -> Arbitraries.integers().between(12, 13)
                                                                                .flatMap(hour -> Arbitraries.integers()
                                                                                                .between(0, 59)
                                                                                                .map(minute -> LocalDateTime
                                                                                                                .of(year, month, day,
                                                                                                                                hour,
                                                                                                                                minute))))));
        }

        @Provide
        Arbitrary<LocalDateTime> breakEndTimes() {
                return Arbitraries.integers().between(2024, 2025)
                                .flatMap(year -> Arbitraries.integers().between(1, 12)
                                                .flatMap(month -> Arbitraries.integers().between(1, 28)
                                                                .flatMap(day -> Arbitraries.integers().between(13, 14)
                                                                                .flatMap(hour -> Arbitraries.integers()
                                                                                                .between(0, 59)
                                                                                                .map(minute -> LocalDateTime
                                                                                                                .of(year, month, day,
                                                                                                                                hour,
                                                                                                                                minute))))));
        }

        @Provide
        Arbitrary<Integer> breakFieldCombinations() {
                // 1 = chỉ breakStart, 2 = chỉ breakEnd, 3 = cả hai
                return Arbitraries.integers().between(1, 3);
        }

        @Provide
        Arbitrary<Long> breakRecordIds() {
                return Arbitraries.longs().between(1L, 100000L);
        }
}
