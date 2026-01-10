package com.tamabee.api_hr.mapper.admin;

import com.tamabee.api_hr.dto.request.wallet.DepositRequestCreateRequest;
import com.tamabee.api_hr.dto.response.wallet.DepositRequestResponse;
import com.tamabee.api_hr.entity.wallet.DepositRequestEntity;
import com.tamabee.api_hr.enums.DepositStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper cho DepositRequest entity
 * Chuyển đổi giữa Entity, Request DTO và Response DTO
 */
@Component
public class DepositRequestMapper {

    /**
     * Chuyển đổi DepositRequestCreateRequest sang DepositRequestEntity
     * Dùng khi tạo yêu cầu nạp tiền mới
     * 
     * @param request     Request DTO
     * @param companyId   ID của company (từ JWT token)
     * @param requestedBy Employee code của người tạo yêu cầu (từ JWT token)
     */
    public DepositRequestEntity toEntity(DepositRequestCreateRequest request, Long companyId, String requestedBy) {
        if (request == null) {
            return null;
        }

        DepositRequestEntity entity = new DepositRequestEntity();
        entity.setCompanyId(companyId);
        entity.setAmount(request.getAmount());
        entity.setTransferProofUrl(request.getTransferProofUrl());
        entity.setStatus(DepositStatus.PENDING);
        entity.setRequestedBy(requestedBy);
        entity.setDeleted(false);

        return entity;
    }

    /**
     * Chuyển đổi DepositRequestEntity sang DepositRequestResponse
     * Không bao gồm companyName, requesterName, requesterEmail, approvedByName (cần
     * lookup riêng)
     */
    public DepositRequestResponse toResponse(DepositRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        DepositRequestResponse response = new DepositRequestResponse();
        response.setId(entity.getId());
        response.setCompanyId(entity.getCompanyId());
        response.setAmount(entity.getAmount());
        response.setTransferProofUrl(entity.getTransferProofUrl());
        response.setStatus(entity.getStatus());
        response.setRequestedBy(entity.getRequestedBy());
        response.setApprovedBy(entity.getApprovedBy());
        response.setRejectionReason(entity.getRejectionReason());
        response.setProcessedAt(entity.getProcessedAt());
        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }

    /**
     * Chuyển đổi DepositRequestEntity sang DepositRequestResponse với thông tin bổ
     * sung
     * 
     * @param entity         Entity cần chuyển đổi
     * @param companyName    Tên company
     * @param requesterName  Tên người tạo yêu cầu (fallback về employee code nếu
     *                       không có name)
     * @param requesterEmail Email người tạo yêu cầu
     * @param approvedByName Tên người duyệt/từ chối (có thể null)
     */
    public DepositRequestResponse toResponse(
            DepositRequestEntity entity,
            String companyName,
            String requesterName,
            String requesterEmail,
            String approvedByName) {

        if (entity == null) {
            return null;
        }

        DepositRequestResponse response = toResponse(entity);
        response.setCompanyName(companyName);
        response.setRequesterName(requesterName);
        response.setRequesterEmail(requesterEmail);
        response.setApprovedByName(approvedByName);

        return response;
    }
}
