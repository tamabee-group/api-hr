package com.tamabee.api_hr.controller.admin;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.enums.DepositStatus;
import com.tamabee.api_hr.enums.FeedbackStatus;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.repository.core.FeedbackRepository;
import com.tamabee.api_hr.repository.wallet.DepositRequestRepository;

import lombok.RequiredArgsConstructor;

/**
 * Controller trả về số lượng chờ xử lý cho sidebar badges (Tamabee admin).
 * GET /api/admin/pending-counts
 */
@RestController
@RequestMapping("/api/admin/pending-counts")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_ALL_TAMABEE_ACCESS)
public class AdminPendingCountsController {

    private final DepositRequestRepository depositRequestRepository;
    private final FeedbackRepository feedbackRepository;

    @GetMapping
    public ResponseEntity<BaseResponse<Map<String, Long>>> getPendingCounts() {
        long pendingDeposits = depositRequestRepository.countByDeletedFalseAndStatus(DepositStatus.PENDING);
        long openFeedbacks = feedbackRepository.countByStatus(FeedbackStatus.RECEIVED);

        return ResponseEntity.ok(BaseResponse.success(Map.of(
                "pendingDeposits", pendingDeposits,
                "openFeedbacks", openFeedbacks)));
    }
}
