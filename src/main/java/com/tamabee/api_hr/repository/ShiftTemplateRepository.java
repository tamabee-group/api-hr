package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplateEntity, Long> {

    /**
     * Tìm shift template theo ID và chưa bị xóa
     */
    Optional<ShiftTemplateEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách shift templates của công ty (phân trang)
     */
    Page<ShiftTemplateEntity> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

    /**
     * Lấy tất cả shift templates của công ty (không phân trang)
     */
    List<ShiftTemplateEntity> findByCompanyIdAndDeletedFalse(Long companyId);

    /**
     * Lấy danh sách shift templates đang active của công ty
     */
    List<ShiftTemplateEntity> findByCompanyIdAndIsActiveTrueAndDeletedFalse(Long companyId);

    /**
     * Kiểm tra shift template có tồn tại không
     */
    boolean existsByIdAndDeletedFalse(Long id);

    /**
     * Kiểm tra shift template có thuộc công ty không
     */
    boolean existsByIdAndCompanyIdAndDeletedFalse(Long id, Long companyId);
}
