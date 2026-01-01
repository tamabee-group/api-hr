# Implementation Plan: Multiple Breaks Support

## Overview

- Triển khai hỗ trợ nhiều break sessions trong 1 ngày làm việc. Các thay đổi bao gồm entity, DTOs, services, và validation logic.

- Khi Kiro thực hiện task hãy phản hồi tôi bằng tiếng việt.

## Tasks

- [x] 1. Database Migration và Entity Changes

  - [x] 1.1 Cập nhật V1\_\_init_schema.sql thêm cột break_number và break_record_id

    - Thêm `break_number INTEGER NOT NULL DEFAULT 1` vào break_records
    - Thêm `break_record_id BIGINT` vào attendance_adjustment_requests
    - Thêm index cho break_number
    - _Requirements: 1.3_

  - [x] 1.2 Cập nhật BreakRecordEntity thêm breakNumber field

    - Thêm field `private Integer breakNumber`
    - _Requirements: 1.3_

  - [x] 1.3 Cập nhật AttendanceAdjustmentRequestEntity thêm breakRecordId field
    - Thêm field `private Long breakRecordId`
    - _Requirements: 2.1_

- [x] 2. DTO Changes

  - [x] 2.1 Cập nhật CreateAdjustmentRequest thêm breakRecordId

    - Thêm field `private Long breakRecordId`
    - _Requirements: 2.1_

  - [x] 2.2 Cập nhật AdjustmentRequestResponse thêm breakRecordId và breakNumber

    - Thêm fields `breakRecordId`, `breakNumber`
    - _Requirements: 5.3, 5.4_

  - [x] 2.3 Cập nhật DailyBreakReportResponse thêm breakSessions list

    - Thêm inner class `BreakSessionInfo`
    - Thêm field `List<BreakSessionInfo> breakSessions`
    - _Requirements: 4.2_

  - [x] 2.4 Cập nhật BreakConfigResponse thêm maxBreaksPerDay
    - Thêm field `private Integer maxBreaksPerDay`
    - _Requirements: 3.4_

- [x] 3. Repository Changes

  - [x] 3.1 Thêm methods vào BreakRecordRepository
    - `findActiveBreakByEmployeeIdAndWorkDate()` - tìm break đang active
    - `countByAttendanceRecordIdAndDeletedFalse()` - đếm số breaks
    - `findMaxBreakNumberByAttendanceRecordId()` - lấy breakNumber lớn nhất
    - _Requirements: 3.1, 3.4, 1.1_

- [x] 4. Service Layer Changes

  - [x] 4.1 Cập nhật BreakServiceImpl.startBreak() với validation và breakNumber

    - Validate không có break đang active
    - Validate chưa đạt maxBreaksPerDay
    - Assign breakNumber = max + 1
    - _Requirements: 1.1, 3.1, 3.4_

  - [x] 4.2 Write property test cho sequential break number assignment

    - **Property 1: Sequential Break Number Assignment**
    - **Validates: Requirements 1.1, 1.3**

  - [x] 4.3 Write property test cho no concurrent active breaks

    - **Property 6: No Concurrent Active Breaks**
    - **Validates: Requirements 3.1**

  - [x] 4.4 Write property test cho max breaks per day enforcement

    - **Property 8: Maximum Breaks Per Day Enforcement**
    - **Validates: Requirements 3.4**

  - [x] 4.5 Thêm method calculateTotalBreakMinutes() vào BreakServiceImpl

    - Tính tổng thời gian break từ tất cả sessions
    - _Requirements: 1.2, 4.3_

  - [x] 4.6 Write property test cho total break duration calculation
    - **Property 2: Total Break Duration Calculation**
    - **Validates: Requirements 1.2, 4.3**

- [x] 5. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Adjustment Service Changes

  - [x] 6.1 Cập nhật AttendanceAdjustmentServiceImpl.createAdjustmentRequest()

    - Validate breakRecordId required khi có break fields
    - Validate breakRecord thuộc về attendanceRecord
    - Lưu breakRecordId vào entity
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 6.2 Write property test cho break record ID required validation

    - **Property 5: Break Record ID Required for Break Adjustment**
    - **Validates: Requirements 2.3**

  - [x] 6.3 Cập nhật AttendanceAdjustmentServiceImpl.approveAdjustment()

    - Chỉ update break record được chỉ định bởi breakRecordId
    - _Requirements: 2.4_

  - [x] 6.4 Write property test cho specific break adjustment
    - **Property 4: Specific Break Adjustment**
    - **Validates: Requirements 2.2, 2.4**

- [x] 7. Mapper Changes

  - [x] 7.1 Cập nhật AttendanceAdjustmentMapper.toResponse()

    - Map breakRecordId và breakNumber vào response
    - _Requirements: 5.3, 5.4_

  - [x] 7.2 Cập nhật BreakRecordMapper để include breakNumber
    - _Requirements: 1.3_

- [x] 8. Report Service Changes

  - [x] 8.1 Cập nhật BreakReportServiceImpl để aggregate multiple breaks

    - Tạo breakSessions list trong DailyBreakReportResponse
    - Tính totalBreakMinutes từ tất cả sessions
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 8.2 Write property test cho breaks ordered by break number
    - **Property 3: Breaks Ordered by Break Number**
    - **Validates: Requirements 1.4, 5.2**

- [x] 9. Payroll Calculator Changes

  - [x] 9.1 Cập nhật PayrollCalculator để sử dụng aggregated break time

    - Gọi calculateTotalBreakMinutes() thay vì lấy từ single break
    - _Requirements: 4.4_

  - [x] 9.2 Write property test cho payroll uses aggregated break time
    - **Property 9: Payroll Uses Aggregated Break Time**
    - **Validates: Requirements 4.4**

- [x] 10. Break Overlap Validation

  - [x] 10.1 Thêm validation cho overlapping breaks trong BreakServiceImpl

    - Check new break start time không nằm trong existing breaks
    - _Requirements: 3.2, 3.3_

  - [x] 10.2 Write property test cho no overlapping break times
    - **Property 7: No Overlapping Break Times**
    - **Validates: Requirements 3.2, 3.3**

- [x] 11. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Cập nhật V1\_\_init_schema.sql thay vì tạo migration mới. gộp v4-v5-v6 vào chung v1.
- Update V2 và V3 đầy đủ dữ liệu các case để test hệ thống.
- Sử dụng jqwik library cho property-based testing
- breakNumber bắt đầu từ 1 (không phải 0)
- maxBreaksPerDay mặc định là 3 nếu không được cấu hình
