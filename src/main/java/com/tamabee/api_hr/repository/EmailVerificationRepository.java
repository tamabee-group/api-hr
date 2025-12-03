package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.core.EmailVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, Long> {
    
    @Query("SELECT e FROM EmailVerificationEntity e WHERE e.email = :email AND e.code = :code AND e.used = false AND e.expiredAt > :now")
    Optional<EmailVerificationEntity> findValidCode(@Param("email") String email, @Param("code") String code, @Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM EmailVerificationEntity e WHERE e.email = :email AND e.used = true ORDER BY e.createdAt DESC")
    List<EmailVerificationEntity> findByEmailAndUsedTrue(@Param("email") String email);
    
    @Query("SELECT e FROM EmailVerificationEntity e WHERE e.email = :email AND e.used = false")
    List<EmailVerificationEntity> findByEmailAndUsedFalse(@Param("email") String email);
    
    void deleteByEmail(String email);
}
