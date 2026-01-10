package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

        boolean existsByEmail(String email);

        boolean existsByName(String name);

        boolean existsByTenantDomainAndDeletedFalse(String tenantDomain);

        Optional<CompanyEntity> findByTenantDomainAndDeletedFalse(String tenantDomain);

        Optional<CompanyEntity> findByEmail(String email);

        Optional<CompanyEntity> findByIdAndDeletedFalse(Long id);

        /**
         * Lấy tất cả companies theo status và chưa bị xóa
         */
        List<CompanyEntity> findAllByStatusAndDeletedFalse(CompanyStatus status);

        /**
         * Lấy danh sách companies được giới thiệu bởi employee (phân trang)
         */
        @Query("SELECT c FROM CompanyEntity c WHERE c.deleted = false " +
                        "AND c.referredByEmployee.id = :employeeId ORDER BY c.createdAt DESC")
        Page<CompanyEntity> findByReferredByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

        /**
         * Đếm số companies được giới thiệu bởi employee
         */
        @Query("SELECT COUNT(c) FROM CompanyEntity c WHERE c.deleted = false " +
                        "AND c.referredByEmployee.id = :employeeId")
        int countByReferredByEmployeeId(@Param("employeeId") Long employeeId);

        /**
         * Tìm các company INACTIVE đã quá thời gian retention (90 ngày)
         * Dùng cho scheduled cleanup job
         */
        @Query("SELECT c FROM CompanyEntity c WHERE c.deleted = false " +
                        "AND c.status = 'INACTIVE' " +
                        "AND c.deactivatedAt IS NOT NULL " +
                        "AND c.deactivatedAt < :cutoffDate")
        List<CompanyEntity> findInactiveCompaniesForCleanup(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
