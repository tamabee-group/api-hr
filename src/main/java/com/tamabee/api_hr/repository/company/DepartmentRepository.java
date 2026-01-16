package com.tamabee.api_hr.repository.company;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.company.DepartmentEntity;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    // Lấy danh sách phòng ban chưa bị xóa
    Page<DepartmentEntity> findByDeletedFalse(Pageable pageable);

    List<DepartmentEntity> findByDeletedFalse();

    // Lấy phòng ban theo id
    Optional<DepartmentEntity> findByIdAndDeletedFalse(Long id);

    // Kiểm tra code đã tồn tại
    boolean existsByCodeAndDeletedFalse(String code);

    // Kiểm tra code đã tồn tại (trừ id hiện tại - dùng khi update)
    boolean existsByCodeAndDeletedFalseAndIdNot(String code, Long id);

    // Lấy danh sách phòng ban con
    List<DepartmentEntity> findByParentIdAndDeletedFalse(Long parentId);

    // Lấy danh sách phòng ban gốc (không có parent)
    List<DepartmentEntity> findByParentIsNullAndDeletedFalse();

    // Đếm số phòng ban con
    long countByParentIdAndDeletedFalse(Long parentId);

    // Đếm tổng số phòng ban (để generate code)
    long countByDeletedFalse();

    // Tìm kiếm theo tên hoặc code
    @Query("SELECT d FROM DepartmentEntity d WHERE d.deleted = false " +
           "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(d.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DepartmentEntity> searchByNameOrCode(@Param("keyword") String keyword, Pageable pageable);
}
