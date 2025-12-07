package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.company.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {
    
    boolean existsByEmail(String email);
    
    boolean existsByName(String name);
    
    Optional<CompanyEntity> findByEmail(String email);
}
