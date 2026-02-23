package com.tamabee.api_hr.entity.attendance;

import java.time.LocalDateTime;

import com.tamabee.api_hr.enums.BreakActionType;
import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity lưu trữ chi tiết điều chỉnh break trong yêu cầu điều chỉnh chấm công.
 * Mỗi request có thể có nhiều break items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "adjustment_break_items", indexes = {
        @Index(name = "idx_adjustment_break_items_request_id", columnList = "adjustment_request_id"),
        @Index(name = "idx_adjustment_break_items_break_record_id", columnList = "breakRecordId")
})
public class AdjustmentBreakItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với request cha
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_request_id", nullable = false)
    private AttendanceAdjustmentRequestEntity adjustmentRequest;

    // ID của break record cần điều chỉnh (null khi actionType = CREATE)
    @Column(nullable = true)
    private Long breakRecordId;

    // Số thứ tự break (để hiển thị)
    private Integer breakNumber;

    // Loại thao tác: ADJUST (điều chỉnh), DELETE (xóa)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BreakActionType actionType;

    // Thời gian break gốc
    private LocalDateTime originalBreakStart;
    private LocalDateTime originalBreakEnd;

    // Thời gian break yêu cầu thay đổi
    private LocalDateTime requestedBreakStart;
    private LocalDateTime requestedBreakEnd;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        }
        if (actionType == null) {
            actionType = BreakActionType.ADJUST;
        }
    }
}
