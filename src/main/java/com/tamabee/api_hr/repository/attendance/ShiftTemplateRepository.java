package com.tamabee.api_hr.repository.attendance;

import com.tamabee.api_hr.entity.attendance.ShiftTemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý mẫu ca làm việc.
 */
@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplateEntity, Long> {

    /**
     * Tìm shift template theo ID và chưa bị xóa
     */
    Optional<ShiftTemplateEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách shift templates (phân trang)
     */
    Page<ShiftTemplateEntity> findByDeletedFalse(Pageable pageable);

    /**
     * Lấy tất cả shift templates (không phân trang)
     */
    List<ShiftTemplateEntity> findByDeletedFalse();

    /**
     * Lấy danh sách shift templates đang active
     */
    List<ShiftTemplateEntity> findByIsActiveTrueAndDeletedFalse();

    /**
     * Kiểm tra shift template có tồn tại không
     */
    boolean existsByIdAndDeletedFalse(Long id);
}
