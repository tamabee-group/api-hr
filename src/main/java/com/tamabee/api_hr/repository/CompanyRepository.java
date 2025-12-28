package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByName(String name);

    Optional<CompanyEntity> findByEmail(String email);

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
}
