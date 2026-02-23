package com.tamabee.api_hr.repository.company;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.enums.CompanyStatus;

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
         * Lấy tất cả companies chưa bị xóa (cả ACTIVE và INACTIVE)
         * Dùng để load tenant DataSources khi startup
         */
        List<CompanyEntity> findAllByDeletedFalse();

        /**
         * Lấy danh sách companies được giới thiệu bởi employee (phân trang)
         */
        @Query("SELECT c FROM CompanyEntity c WHERE c.deleted = false " +
                        "AND c.referredByEmployeeId = :employeeId ORDER BY c.createdAt DESC")
        Page<CompanyEntity> findByReferredByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

        /**
         * Đếm số companies được giới thiệu bởi employee
         */
        @Query("SELECT COUNT(c) FROM CompanyEntity c WHERE c.deleted = false " +
                        "AND c.referredByEmployeeId = :employeeId")
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

        /**
         * Insert Tamabee company với id = 0 (đặc biệt)
         * Dùng native query vì JPA không cho phép set id thủ công
         */
        @Modifying
        @Query(value = "INSERT INTO companies (id, name, owner_name, email, phone, address, industry, zipcode, " +
                        "region, language, tenant_domain, plan_id, status, deleted, created_at, updated_at) " +
                        "VALUES (0, :name, :ownerName, :email, :phone, :address, :industry, :zipcode, " +
                        ":region, :language, :tenantDomain, :planId, :status, false, NOW(), NOW()) " +
                        "ON CONFLICT (id) DO NOTHING", nativeQuery = true)
        void insertTamabeeCompany(
                        @Param("name") String name,
                        @Param("ownerName") String ownerName,
                        @Param("email") String email,
                        @Param("phone") String phone,
                        @Param("address") String address,
                        @Param("industry") String industry,
                        @Param("zipcode") String zipcode,
                        @Param("region") String region,
                        @Param("language") String language,
                        @Param("tenantDomain") String tenantDomain,
                        @Param("planId") Long planId,
                        @Param("status") String status);
}
