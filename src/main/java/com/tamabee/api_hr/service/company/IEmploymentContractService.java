package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.ContractQuery;
import com.tamabee.api_hr.dto.request.ContractRequest;
import com.tamabee.api_hr.dto.response.ContractResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface cho quản lý hợp đồng lao động
 */
public interface IEmploymentContractService {

    /**
     * Tạo hợp đồng mới cho nhân viên
     *
     * @param employeeId ID nhân viên
     * @param request    Thông tin hợp đồng
     * @return Thông tin hợp đồng đã tạo
     */
    ContractResponse createContract(Long employeeId, ContractRequest request);

    /**
     * Cập nhật thông tin hợp đồng
     *
     * @param contractId ID hợp đồng
     * @param request    Thông tin cập nhật
     * @return Thông tin hợp đồng đã cập nhật
     */
    ContractResponse updateContract(Long contractId, ContractRequest request);

    /**
     * Chấm dứt hợp đồng
     *
     * @param contractId ID hợp đồng
     * @param reason     Lý do chấm dứt
     * @return Thông tin hợp đồng đã chấm dứt
     */
    ContractResponse terminateContract(Long contractId, String reason);

    /**
     * Lấy hợp đồng hiện tại của nhân viên
     *
     * @param employeeId ID nhân viên
     * @return Thông tin hợp đồng hiện tại
     */
    ContractResponse getCurrentContract(Long employeeId);

    /**
     * Lấy lịch sử hợp đồng của nhân viên
     *
     * @param employeeId ID nhân viên
     * @return Danh sách lịch sử hợp đồng
     */
    List<ContractResponse> getContractHistory(Long employeeId);

    /**
     * Lấy danh sách hợp đồng sắp hết hạn
     *
     * @param companyId       ID công ty
     * @param daysUntilExpiry Số ngày còn lại đến khi hết hạn
     * @param pageable        Phân trang
     * @return Danh sách hợp đồng sắp hết hạn
     */
    Page<ContractResponse> getExpiringContracts(Long companyId, int daysUntilExpiry, Pageable pageable);

    /**
     * Lấy danh sách hợp đồng của công ty với filter
     *
     * @param companyId ID công ty
     * @param query     Điều kiện lọc
     * @param pageable  Phân trang
     * @return Danh sách hợp đồng
     */
    Page<ContractResponse> getContracts(Long companyId, ContractQuery query, Pageable pageable);

    /**
     * Lấy chi tiết hợp đồng theo ID
     *
     * @param contractId ID hợp đồng
     * @return Thông tin hợp đồng
     */
    ContractResponse getContractById(Long contractId);
}
