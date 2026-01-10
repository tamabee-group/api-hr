package com.tamabee.api_hr.controller.company;

import com.tamabee.api_hr.dto.request.ContractQuery;
import com.tamabee.api_hr.dto.request.ContractRequest;
import com.tamabee.api_hr.dto.response.ContractResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ContractStatus;
import com.tamabee.api_hr.enums.ContractType;
import com.tamabee.api_hr.enums.RoleConstants;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.repository.UserRepository;
import com.tamabee.api_hr.service.company.IEmploymentContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller quản lý hợp đồng lao động.
 * ADMIN_COMPANY và MANAGER_COMPANY có quyền truy cập.
 */
@RestController
@RequestMapping("/api/company/contracts")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.HAS_COMPANY_ACCESS)
public class EmploymentContractController {

    private final IEmploymentContractService contractService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách hợp đồng của công ty với filter
     * GET /api/company/contracts
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<ContractResponse>>> getContracts(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) ContractType contractType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ContractQuery query = new ContractQuery();
        query.setStatus(status);
        query.setContractType(contractType);
        query.setStartDateFrom(startDateFrom);
        query.setStartDateTo(startDateTo);
        query.setEmployeeId(employeeId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        Page<ContractResponse> contracts = contractService.getContracts(query, pageable);
        return ResponseEntity.ok(BaseResponse.success(contracts, "Lấy danh sách hợp đồng thành công"));
    }

    /**
     * Lấy danh sách hợp đồng sắp hết hạn
     * GET /api/company/contracts/expiring
     */
    @GetMapping("/expiring")
    public ResponseEntity<BaseResponse<Page<ContractResponse>>> getExpiringContracts(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "endDate"));
        Page<ContractResponse> contracts = contractService.getExpiringContracts(days, pageable);
        return ResponseEntity.ok(BaseResponse.success(contracts, "Lấy danh sách hợp đồng sắp hết hạn thành công"));
    }

    /**
     * Lấy chi tiết hợp đồng theo ID
     * GET /api/company/contracts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ContractResponse>> getContractById(@PathVariable Long id) {
        ContractResponse contract = contractService.getContractById(id);
        return ResponseEntity.ok(BaseResponse.success(contract, "Lấy thông tin hợp đồng thành công"));
    }

    /**
     * Lấy hợp đồng hiện tại của nhân viên
     * GET /api/company/employees/{employeeId}/contracts/current
     */
    @GetMapping("/employees/{employeeId}/current")
    public ResponseEntity<BaseResponse<ContractResponse>> getCurrentContract(@PathVariable Long employeeId) {
        ContractResponse contract = contractService.getCurrentContract(employeeId);
        return ResponseEntity.ok(BaseResponse.success(contract, "Lấy hợp đồng hiện tại thành công"));
    }

    /**
     * Lấy lịch sử hợp đồng của nhân viên
     * GET /api/company/employees/{employeeId}/contracts/history
     */
    @GetMapping("/employees/{employeeId}/history")
    public ResponseEntity<BaseResponse<List<ContractResponse>>> getContractHistory(@PathVariable Long employeeId) {
        List<ContractResponse> history = contractService.getContractHistory(employeeId);
        return ResponseEntity.ok(BaseResponse.success(history, "Lấy lịch sử hợp đồng thành công"));
    }

    /**
     * Tạo hợp đồng mới cho nhân viên
     * POST /api/company/employees/{employeeId}/contracts
     */
    @PostMapping("/employees/{employeeId}")
    public ResponseEntity<BaseResponse<ContractResponse>> createContract(
            @PathVariable Long employeeId,
            @Valid @RequestBody ContractRequest request) {
        ContractResponse response = contractService.createContract(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response, "Tạo hợp đồng thành công"));
    }

    /**
     * Cập nhật thông tin hợp đồng
     * PUT /api/company/contracts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<ContractResponse>> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody ContractRequest request) {
        ContractResponse response = contractService.updateContract(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật hợp đồng thành công"));
    }

    /**
     * Chấm dứt hợp đồng
     * POST /api/company/contracts/{id}/terminate
     */
    @PostMapping("/{id}/terminate")
    public ResponseEntity<BaseResponse<ContractResponse>> terminateContract(
            @PathVariable Long id,
            @RequestParam String reason) {
        ContractResponse response = contractService.terminateContract(id, reason);
        return ResponseEntity.ok(BaseResponse.success(response, "Chấm dứt hợp đồng thành công"));
    }
}
