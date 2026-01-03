package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.contract.EmploymentContractEntity;
import com.tamabee.api_hr.enums.ContractStatus;
import com.tamabee.api_hr.enums.ContractType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmploymentContractRepository extends JpaRepository<EmploymentContractEntity, Long> {

        /**
         * Tìm employment contract theo ID và chưa bị xóa
         */
        Optional<EmploymentContractEntity> findByIdAndDeletedFalse(Long id);

        /**
         * Lấy contract đang active của nhân viên
         */
        @Query("SELECT ec FROM EmploymentContractEntity ec " +
                        "WHERE ec.deleted = false " +
                        "AND ec.employeeId = :employeeId " +
                        "AND ec.status = 'ACTIVE' " +
                        "AND ec.startDate <= :currentDate " +
                        "AND (ec.endDate IS NULL OR ec.endDate >= :currentDate)")
        Optional<EmploymentContractEntity> findActiveByEmployeeId(
                        @Param("employeeId") Long employeeId,
                        @Param("currentDate") LocalDate currentDate);

        /**
         * Lấy tất cả contracts của nhân viên
         */
        List<EmploymentContractEntity> findByEmployeeIdAndDeletedFalseOrderByStartDateDesc(Long employeeId);

        /**
         * Lấy contracts của nhân viên theo status
         */
        List<EmploymentContractEntity> findByEmployeeIdAndStatusAndDeletedFalseOrderByStartDateDesc(
                        Long employeeId, ContractStatus status);

        /**
         * Lấy danh sách contracts của công ty (phân trang)
         */
        Page<EmploymentContractEntity> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

        /**
         * Lấy contracts của công ty theo status
         */
        Page<EmploymentContractEntity> findByCompanyIdAndStatusAndDeletedFalse(
                        Long companyId, ContractStatus status, Pageable pageable);

        /**
         * Lấy contracts của công ty theo contract type
         */
        Page<EmploymentContractEntity> findByCompanyIdAndContractTypeAndDeletedFalse(
                        Long companyId, ContractType contractType, Pageable pageable);

        /**
         * Lấy contracts sắp hết hạn (trong vòng N ngày)
         */
        @Query("SELECT ec FROM EmploymentContractEntity ec " +
                        "WHERE ec.deleted = false " +
                        "AND ec.companyId = :companyId " +
                        "AND ec.status = 'ACTIVE' " +
                        "AND ec.endDate IS NOT NULL " +
                        "AND ec.endDate BETWEEN :currentDate AND :expiryDate " +
                        "ORDER BY ec.endDate ASC")
        Page<EmploymentContractEntity> findExpiringContracts(
                        @Param("companyId") Long companyId,
                        @Param("currentDate") LocalDate currentDate,
                        @Param("expiryDate") LocalDate expiryDate,
                        Pageable pageable);

        /**
         * Lấy contracts đã hết hạn nhưng chưa được xử lý
         */
        @Query("SELECT ec FROM EmploymentContractEntity ec " +
                        "WHERE ec.deleted = false " +
                        "AND ec.status = 'ACTIVE' " +
                        "AND ec.endDate IS NOT NULL " +
                        "AND ec.endDate < :currentDate")
        List<EmploymentContractEntity> findExpiredActiveContracts(@Param("currentDate") LocalDate currentDate);

        /**
         * Kiểm tra có contract overlap không
         */
        @Query("SELECT COUNT(ec) > 0 FROM EmploymentContractEntity ec " +
                        "WHERE ec.deleted = false " +
                        "AND ec.employeeId = :employeeId " +
                        "AND ec.id != :excludeId " +
                        "AND ec.status != 'TERMINATED' " +
                        "AND ec.startDate <= :endDate " +
                        "AND (ec.endDate IS NULL OR ec.endDate >= :startDate)")
        boolean existsOverlappingContract(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("excludeId") Long excludeId);

        /**
         * Kiểm tra nhân viên có contract active không
         */
        @Query("SELECT COUNT(ec) > 0 FROM EmploymentContractEntity ec " +
                        "WHERE ec.deleted = false " +
                        "AND ec.employeeId = :employeeId " +
                        "AND ec.status = 'ACTIVE' " +
                        "AND ec.startDate <= :currentDate " +
                        "AND (ec.endDate IS NULL OR ec.endDate >= :currentDate)")
        boolean hasActiveContract(
                        @Param("employeeId") Long employeeId,
                        @Param("currentDate") LocalDate currentDate);

        /**
         * Lấy contract theo contract number
         */
        Optional<EmploymentContractEntity> findByContractNumberAndDeletedFalse(String contractNumber);

        /**
         * Kiểm tra contract number đã tồn tại chưa
         */
        boolean existsByContractNumberAndDeletedFalse(String contractNumber);

        /**
         * Đếm số contracts theo status
         */
        long countByCompanyIdAndStatusAndDeletedFalse(Long companyId, ContractStatus status);

        /**
         * Lấy contracts trong khoảng thời gian
         */
        @Query("SELECT ec FROM EmploymentContractEntity ec " +
                        "WHERE ec.deleted = false " +
                        "AND ec.companyId = :companyId " +
                        "AND ec.startDate <= :endDate " +
                        "AND (ec.endDate IS NULL OR ec.endDate >= :startDate) " +
                        "ORDER BY ec.startDate DESC")
        List<EmploymentContractEntity> findByCompanyIdAndDateRange(
                        @Param("companyId") Long companyId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Lấy contracts active trong khoảng thời gian (dùng cho report)
         */
        @Query("SELECT ec FROM EmploymentContractEntity ec " +
                        "WHERE ec.deleted = false " +
                        "AND ec.companyId = :companyId " +
                        "AND ec.status = 'ACTIVE' " +
                        "AND ec.startDate <= :endDate " +
                        "AND (ec.endDate IS NULL OR ec.endDate >= :startDate)")
        List<EmploymentContractEntity> findActiveContractsByCompanyIdAndDateRange(
                        @Param("companyId") Long companyId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Đếm số contracts của công ty trong năm (dùng để generate contract number)
         */
        @Query("SELECT COUNT(ec) FROM EmploymentContractEntity ec " +
                        "WHERE ec.companyId = :companyId " +
                        "AND YEAR(ec.createdAt) = :year")
        long countByCompanyIdAndYear(
                        @Param("companyId") Long companyId,
                        @Param("year") int year);
}
