package com.tamabee.api_hr.repository.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.user.EmployeeDocumentEntity;

/**
 * Repository cho EmployeeDocumentEntity.
 * Không có soft delete - xóa thẳng.
 */
@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocumentEntity, Long> {

    /**
     * Lấy danh sách documents của nhân viên với pagination
     */
    Page<EmployeeDocumentEntity> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Tìm document theo ID và employeeId
     */
    Optional<EmployeeDocumentEntity> findByIdAndEmployeeId(Long id, Long employeeId);

    /**
     * Đếm số documents của nhân viên
     */
    long countByEmployeeId(Long employeeId);
}
