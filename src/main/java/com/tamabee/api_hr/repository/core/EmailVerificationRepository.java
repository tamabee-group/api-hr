package com.tamabee.api_hr.repository.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.core.EmailVerificationEntity;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, Long> {
    
    @Query("SELECT e FROM EmailVerificationEntity e WHERE e.email = :email AND e.code = :code AND e.used = false AND e.expiredAt > :now")
    Optional<EmailVerificationEntity> findValidCode(@Param("email") String email, @Param("code") String code, @Param("now") LocalDateTime now);
    
    /**
     * Tìm token hợp lệ (dùng cho reset password)
     */
    @Query("SELECT e FROM EmailVerificationEntity e WHERE e.code = :token AND e.used = false AND e.expiredAt > :now")
    Optional<EmailVerificationEntity> findValidCode(@Param("token") String token, @Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM EmailVerificationEntity e WHERE e.email = :email AND e.used = true ORDER BY e.createdAt DESC")
    List<EmailVerificationEntity> findByEmailAndUsedTrue(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EmailVerificationEntity e WHERE e.email = :email AND e.used = true")
    boolean existsByEmailAndUsedTrue(@Param("email") String email);
    
    @Query("SELECT e FROM EmailVerificationEntity e WHERE e.email = :email AND e.used = false")
    List<EmailVerificationEntity> findByEmailAndUsedFalse(@Param("email") String email);
    
    void deleteByEmail(String email);
}
