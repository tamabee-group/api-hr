package com.tamabee.api_hr.mapper.admin;

import com.tamabee.api_hr.dto.response.CommissionResponse;
import com.tamabee.api_hr.entity.wallet.EmployeeCommissionEntity;
import com.tamabee.api_hr.enums.CommissionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper cho EmployeeCommission entity
 * Chuyển đổi giữa Entity và Response DTO
 */
@Component
public class EmployeeCommissionMapper {

    /**
     * Tạo EmployeeCommissionEntity mới
     * Dùng khi tính hoa hồng cho nhân viên Tamabee
     * 
     * @param employeeCode Employee code của nhân viên Tamabee nhận hoa hồng
     * @param companyId    ID của company được giới thiệu
     * @param amount       Số tiền hoa hồng cố định (JPY)
     */
    public EmployeeCommissionEntity createEntity(
            String employeeCode,
            Long companyId,
            BigDecimal amount) {

        EmployeeCommissionEntity entity = new EmployeeCommissionEntity();
        entity.setEmployeeCode(employeeCode);
        entity.setCompanyId(companyId);
        entity.setAmount(amount);
        entity.setStatus(CommissionStatus.PENDING);

        return entity;
    }

    /**
     * Chuyển đổi EmployeeCommissionEntity sang CommissionResponse
     * Không bao gồm employeeName, companyName, paidByName (cần lookup riêng)
     */
    public CommissionResponse toResponse(EmployeeCommissionEntity entity) {
        if (entity == null) {
            return null;
        }

        CommissionResponse response = new CommissionResponse();
        response.setId(entity.getId());
        response.setEmployeeCode(entity.getEmployeeCode());
        response.setCompanyId(entity.getCompanyId());
        response.setAmount(entity.getAmount());
        response.setStatus(entity.getStatus());
        response.setCompanyBillingAtCreation(entity.getCompanyBillingAtCreation());
        response.setPaidAt(entity.getPaidAt());
        response.setPaidBy(entity.getPaidBy());
        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }

    /**
     * Chuyển đổi EmployeeCommissionEntity sang CommissionResponse với thông tin bổ
     * sung
     * 
     * @param entity       Entity cần chuyển đổi
     * @param employeeName Tên nhân viên Tamabee nhận hoa hồng
     * @param companyName  Tên company được giới thiệu
     * @param paidByName   Tên người thanh toán hoa hồng (có thể null)
     */
    public CommissionResponse toResponse(
            EmployeeCommissionEntity entity,
            String employeeName,
            String companyName,
            String paidByName) {

        if (entity == null) {
            return null;
        }

        CommissionResponse response = toResponse(entity);
        response.setEmployeeName(employeeName);
        response.setCompanyName(companyName);
        response.setPaidByName(paidByName);

        return response;
    }
}
