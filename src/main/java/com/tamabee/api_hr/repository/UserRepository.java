package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByProfileReferralCode(String referralCode);

    Page<UserEntity> findByCompanyId(Long companyId, Pageable pageable);

    long countByCompanyId(Long companyId);
}
