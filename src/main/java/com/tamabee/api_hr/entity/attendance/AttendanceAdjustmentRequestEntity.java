package com.tamabee.api_hr.entity.attendance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tamabee.api_hr.entity.BaseEntity;
import com.tamabee.api_hr.enums.AdjustmentRequestType;
import com.tamabee.api_hr.enums.AdjustmentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity lưu trữ yêu cầu điều chỉnh chấm công của nhân viên.
 * Nhân viên có thể yêu cầu thay đổi giờ check-in/check-out và cần manager phê duyệt.
 * Hỗ trợ điều chỉnh nhiều break records trong 1 request thông qua breakItems.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "attendance_adjustment_requests", indexes = {
        @Index(name = "idx_adjustment_employee_id", columnList = "employeeId"),
        @Index(name = "idx_adjustment_record_id", columnList = "attendanceRecordId"),
        @Index(name = "idx_adjustment_assigned_to", columnList = "assignedTo"),
        @Index(name = "idx_adjustment_status", columnList = "status"),
        @Index(name = "idx_adjustment_work_date", columnList = "workDate"),
        @Index(name = "idx_adjustment_request_type", columnList = "requestType")
})
public class AttendanceAdjustmentRequestEntity extends BaseEntity {

    // ID nhân viên yêu cầu
    @Column(nullable = false)
    private Long employeeId;

    // ID bản ghi chấm công cần điều chỉnh (nullable khi tạo yêu cầu cho ngày chưa có chấm công)
    private Long attendanceRecordId;

    // Ngày làm việc cần điều chỉnh (dùng khi không có attendanceRecordId)
    private java.time.LocalDate workDate;

    // ID người được gán xử lý yêu cầu (manager/admin)
    private Long assignedTo;

    // Loại yêu cầu: ADJUST, DELETE_RECORD
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdjustmentRequestType requestType = AdjustmentRequestType.ADJUST;

    // === Thời gian gốc ===
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;

    // === Thời gian yêu cầu thay đổi ===
    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;

    // Danh sách các break items cần điều chỉnh (hỗ trợ nhiều break trong 1 request)
    @OneToMany(mappedBy = "adjustmentRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdjustmentBreakItemEntity> breakItems = new ArrayList<>();

    // Lý do yêu cầu điều chỉnh
    @Column(length = 500, nullable = false)
    private String reason;

    // === Trạng thái ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdjustmentStatus status = AdjustmentStatus.PENDING;

    // === Approval info ===
    // Người phê duyệt/từ chối
    private Long approvedBy;

    // Thời gian phê duyệt/từ chối
    private LocalDateTime approvedAt;

    // Ghi chú của người phê duyệt
    @Column(length = 500)
    private String approverComment;

    // Lý do từ chối (nếu bị từ chối)
    @Column(length = 500)
    private String rejectionReason;

    // Helper method để thêm break item
    public void addBreakItem(AdjustmentBreakItemEntity item) {
        breakItems.add(item);
        item.setAdjustmentRequest(this);
    }

    // Helper method để thêm nhiều break items
    public void addBreakItems(List<AdjustmentBreakItemEntity> items) {
        for (AdjustmentBreakItemEntity item : items) {
            addBreakItem(item);
        }
    }
}
