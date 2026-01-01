package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.WorkScheduleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý lịch làm việc của công ty.
 */
@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkScheduleEntity, Long> {

    /**
     * Tìm lịch làm việc theo ID (chưa bị xóa)
     */
    Optional<WorkScheduleEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách lịch làm việc của công ty (phân trang)
     */
    Page<WorkScheduleEntity> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

    /**
     * Tìm lịch làm việc mặc định của công ty
     */
    @Query("SELECT w FROM WorkScheduleEntity w WHERE w.deleted = false " +
            "AND w.companyId = :companyId AND w.isDefault = true")
    Optional<WorkScheduleEntity> findDefaultByCompanyId(@Param("companyId") Long companyId);

    /**
     * Kiểm tra tên lịch đã tồn tại trong công ty chưa
     */
    boolean existsByCompanyIdAndNameAndDeletedFalse(Long companyId, String name);

    /**
     * Đếm số lịch làm việc của công ty
     */
    long countByCompanyIdAndDeletedFalse(Long companyId);
}
