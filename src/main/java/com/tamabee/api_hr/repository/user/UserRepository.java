package com.tamabee.api_hr.repository.user;

import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.UserRole;

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

    // Tìm user theo ID (chưa bị xóa)
    Optional<UserEntity> findByIdAndDeletedFalse(Long id);

    // Multi-tenant: Lấy tất cả users trong tenant hiện tại (có phân trang)
    @EntityGraph(attributePaths = { "profile" })
    Page<UserEntity> findByDeletedFalse(Pageable pageable);

    // Multi-tenant: Lấy tất cả users trong tenant hiện tại (không phân trang)
    @EntityGraph(attributePaths = { "profile" })
    List<UserEntity> findByDeletedFalse();

    // Multi-tenant: Đếm users trong tenant hiện tại
    long countByDeletedFalse();

    // Multi-tenant: Lấy danh sách user theo roles (dùng cho lấy approvers)
    @EntityGraph(attributePaths = { "profile" })
    List<UserEntity> findByRoleInAndDeletedFalse(List<UserRole> roles);

    // Legacy methods (không filter deleted - dùng cho internal)
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
