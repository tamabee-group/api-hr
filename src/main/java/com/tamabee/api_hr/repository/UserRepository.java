package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    long countByCompanyIdAndDeletedFalse(Long companyId);

    // Legacy methods (không filter deleted - dùng cho internal)
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
