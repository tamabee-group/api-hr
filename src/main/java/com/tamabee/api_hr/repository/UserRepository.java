package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    Optional<UserEntity> findByEmployeeCodeAndDeletedFalse(String employeeCode);

    // Fetch profile cùng với user theo employeeCode để lấy requester info
    @EntityGraph(attributePaths = { "profile" })
    Optional<UserEntity> findWithProfileByEmployeeCodeAndDeletedFalse(String employeeCode);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByEmployeeCodeAndDeletedFalse(String employeeCode);

    boolean existsByProfileReferralCodeAndDeletedFalse(String referralCode);

    // Fetch profile cùng với user để tránh lazy loading issue
    @EntityGraph(attributePaths = { "profile" })
    Page<UserEntity> findByCompanyIdAndDeletedFalse(Long companyId, Pageable pageable);

    // Lấy tất cả nhân viên của công ty (không phân trang)
    @EntityGraph(attributePaths = { "profile" })
    List<UserEntity> findByCompanyIdAndDeletedFalse(Long companyId);

    // Tìm user theo ID (chưa bị xóa)
    Optional<UserEntity> findByIdAndDeletedFalse(Long id);

    long countByCompanyIdAndDeletedFalse(Long companyId);

    // Lấy danh sách user theo companyId và roles (dùng cho lấy approvers)
    @EntityGraph(attributePaths = { "profile" })
    List<UserEntity> findByCompanyIdAndRoleInAndDeletedFalse(Long companyId,
            List<com.tamabee.api_hr.enums.UserRole> roles);

    // Legacy methods (không filter deleted - dùng cho internal)
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
