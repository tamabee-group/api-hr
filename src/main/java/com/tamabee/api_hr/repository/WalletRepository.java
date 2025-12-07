package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.wallet.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, Long> {
    
    Optional<WalletEntity> findByCompanyId(Long companyId);
    
    boolean existsByCompanyId(Long companyId);
}
