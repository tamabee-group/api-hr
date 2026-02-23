package com.tamabee.api_hr.service.company.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.request.payroll.ContractQuery;
import com.tamabee.api_hr.dto.request.payroll.ContractRequest;
import com.tamabee.api_hr.dto.response.payroll.ContractResponse;
import com.tamabee.api_hr.entity.contract.EmploymentContractEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ContractStatus;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.UserStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.ConflictException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.EmploymentContractMapper;
import com.tamabee.api_hr.repository.contract.EmploymentContractRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IEmploymentContractService;

import lombok.RequiredArgsConstructor;

/**
 * Service implementation cho quản lý hợp đồng lao động
 */
@Service
@RequiredArgsConstructor
public class EmploymentContractServiceImpl implements IEmploymentContractService {

    private final EmploymentContractRepository contractRepository;
    private final UserRepository userRepository;
    private final EmploymentContractMapper contractMapper;

    /**
     * Cập nhật trạng thái nhân viên dựa trên hợp đồng
     * - Có hợp đồng ACTIVE -> status = ACTIVE
     * - Không có hợp đồng ACTIVE -> status = INACTIVE
     */
    private void updateEmployeeStatus(Long employeeId) {
        UserEntity employee = userRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return;
        }

        // Kiểm tra có hợp đồng ACTIVE không
        LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        boolean hasActiveContract = contractRepository.findActiveByEmployeeId(employeeId, today).isPresent();

        // Cập nhật status
        UserStatus newStatus = hasActiveContract ? UserStatus.ACTIVE : UserStatus.INACTIVE;
        if (employee.getStatus() != newStatus) {
            employee.setStatus(newStatus);
            userRepository.save(employee);
        }
    }

    @Override
    @Transactional
    public ContractResponse createContract(Long employeeId, ContractRequest request) {
        // Validate employee exists
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Validate date range
        validateDateRange(request);

        // Validate no overlapping contracts
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.of(9999, 12, 31);
        if (contractRepository.existsOverlappingContract(employeeId, request.getStartDate(), endDate, 0L)) {
            throw new ConflictException(ErrorCode.CONTRACT_OVERLAP_EXISTS);
        }

        // Tạo entity mới
        EmploymentContractEntity entity = contractMapper.toEntity(request, employeeId);

        // Generate contract number: HD-{year}-{sequence}
        String contractNumber = generateContractNumber();
        entity.setContractNumber(contractNumber);

        // Kiểm tra nếu endDate trong quá khứ thì set status = EXPIRED
        LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        if (request.getEndDate() != null && request.getEndDate().isBefore(today)) {
            entity.setStatus(ContractStatus.EXPIRED);
        }

        // Lưu vào database
        entity = contractRepository.save(entity);

        // Cập nhật trạng thái nhân viên
        updateEmployeeStatus(employeeId);

        return contractMapper.toResponse(entity, employee);
    }

    /**
     * Generate số hợp đồng tự động: HD-{year}-{sequence}
     * Ví dụ: HD-2026-0001
     */
    private String generateContractNumber() {
        int currentYear = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())).getYear();
        long count = contractRepository.countByYear(currentYear);
        return String.format("HD-%d-%04d", currentYear, count + 1);
    }

    @Override
    @Transactional
    public ContractResponse updateContract(Long contractId, ContractRequest request) {
        // Tìm contract hiện tại
        EmploymentContractEntity contract = contractRepository.findByIdAndDeletedFalse(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND));

        // Không cho phép cập nhật contract đã terminated
        if (contract.getStatus() == ContractStatus.TERMINATED) {
            throw new BadRequestException(ErrorCode.CONTRACT_ALREADY_TERMINATED);
        }

        // Validate employee exists
        UserEntity employee = userRepository.findById(contract.getEmployeeId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Validate date range
        validateDateRange(request);

        // Validate contract number uniqueness (nếu thay đổi)
        if (request.getContractNumber() != null && !request.getContractNumber().isBlank()) {
            if (!request.getContractNumber().equals(contract.getContractNumber())) {
                if (contractRepository.existsByContractNumberAndDeletedFalse(request.getContractNumber())) {
                    throw new ConflictException(ErrorCode.CONTRACT_NUMBER_EXISTS);
                }
            }
        }

        // Validate no overlapping contracts (exclude current contract)
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.of(9999, 12, 31);
        if (contractRepository.existsOverlappingContract(contract.getEmployeeId(), request.getStartDate(), endDate,
                contractId)) {
            throw new ConflictException(ErrorCode.CONTRACT_OVERLAP_EXISTS);
        }

        // Cập nhật entity
        contractMapper.updateEntity(contract, request);

        // Kiểm tra nếu endDate trong quá khứ và contract đang ACTIVE thì chuyển sang EXPIRED
        LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        if (contract.getStatus() == ContractStatus.ACTIVE && 
            request.getEndDate() != null && 
            request.getEndDate().isBefore(today)) {
            contract.setStatus(ContractStatus.EXPIRED);
        }

        // Lưu vào database
        contract = contractRepository.save(contract);

        // Cập nhật trạng thái nhân viên
        updateEmployeeStatus(contract.getEmployeeId());

        return contractMapper.toResponse(contract, employee);
    }

    @Override
    @Transactional
    public ContractResponse terminateContract(Long contractId, String reason) {
        // Tìm contract
        EmploymentContractEntity contract = contractRepository.findByIdAndDeletedFalse(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND));

        // Không cho phép terminate contract đã terminated
        if (contract.getStatus() == ContractStatus.TERMINATED) {
            throw new BadRequestException(ErrorCode.CONTRACT_ALREADY_TERMINATED);
        }

        // Validate employee exists
        UserEntity employee = userRepository.findById(contract.getEmployeeId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Cập nhật trạng thái
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setTerminationReason(reason);
        contract.setTerminatedAt(LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion())));

        // Lưu vào database
        contract = contractRepository.save(contract);

        // Cập nhật trạng thái nhân viên
        updateEmployeeStatus(contract.getEmployeeId());

        return contractMapper.toResponse(contract, employee);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getCurrentContract(Long employeeId) {
        // Validate employee exists
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Tìm contract hiện tại
        LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        return contractRepository.findActiveByEmployeeId(employeeId, today)
                .map(contract -> contractMapper.toResponse(contract, employee))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getContractHistory(Long employeeId) {
        // Validate employee exists
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Lấy tất cả contracts của employee
        List<EmploymentContractEntity> contracts = contractRepository
                .findByEmployeeIdAndDeletedFalseOrderByStartDateDesc(employeeId);

        return contracts.stream()
                .map(contract -> contractMapper.toResponse(contract, employee))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContractResponse> getExpiringContracts(int daysUntilExpiry, Pageable pageable) {
        LocalDate today = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        LocalDate expiryDate = today.plusDays(daysUntilExpiry);

        Page<EmploymentContractEntity> contracts = contractRepository
                .findExpiringContracts(today, expiryDate, pageable);

        return contracts.map(contract -> {
            UserEntity employee = userRepository.findById(contract.getEmployeeId()).orElse(null);
            return contractMapper.toResponse(contract, employee);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContractResponse> getContracts(ContractQuery query, Pageable pageable) {
        Page<EmploymentContractEntity> contracts;

        // Filter theo status nếu có
        if (query != null && query.getStatus() != null) {
            contracts = contractRepository.findByStatusAndDeletedFalse(query.getStatus(), pageable);
        } else if (query != null && query.getContractType() != null) {
            contracts = contractRepository.findByContractTypeAndDeletedFalse(query.getContractType(), pageable);
        } else {
            contracts = contractRepository.findByDeletedFalse(pageable);
        }

        // Map sang response và sort theo employeeCode, contractNumber
        return contracts.map(contract -> {
            UserEntity employee = userRepository.findById(contract.getEmployeeId()).orElse(null);
            return contractMapper.toResponse(contract, employee);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractById(Long contractId) {
        EmploymentContractEntity contract = contractRepository.findByIdAndDeletedFalse(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND));

        UserEntity employee = userRepository.findById(contract.getEmployeeId()).orElse(null);
        return contractMapper.toResponse(contract, employee);
    }

    @Override
    @Transactional
    public void deleteContract(Long contractId) {
        // Tìm contract
        EmploymentContractEntity contract = contractRepository.findByIdAndDeletedFalse(contractId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONTRACT_NOT_FOUND));

        // Chỉ cho phép xóa contract đã hết hiệu lực (TERMINATED hoặc EXPIRED)
        if (contract.getStatus() == ContractStatus.ACTIVE) {
            throw new BadRequestException(ErrorCode.CONTRACT_CANNOT_DELETE_ACTIVE);
        }

        Long employeeId = contract.getEmployeeId();

        // Soft delete
        contract.setDeleted(true);
        contractRepository.save(contract);

        // Cập nhật trạng thái nhân viên
        updateEmployeeStatus(employeeId);
    }

    /**
     * Validate date range của contract
     */
    private void validateDateRange(ContractRequest request) {
        if (request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException(ErrorCode.CONTRACT_INVALID_DATE_RANGE);
        }
    }
}
