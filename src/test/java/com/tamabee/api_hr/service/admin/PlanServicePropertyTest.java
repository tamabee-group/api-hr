package com.tamabee.api_hr.service.admin;

import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.admin.PlanFeatureMapper;
import com.tamabee.api_hr.mapper.admin.PlanMapper;
import com.tamabee.api_hr.repository.PlanFeatureRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.service.admin.impl.PlanServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho PlanService
 * Feature: wallet-management, Property 8: Plan Deletion Protection
 * Validates: Requirements 1.8
 */
public class PlanServicePropertyTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanFeatureRepository planFeatureRepository;

    private PlanMapper planMapper;
    private PlanFeatureMapper planFeatureMapper;
    private PlanServiceImpl planService;

    @BeforeTry
    void setUp() {
        MockitoAnnotations.openMocks(this);
        planFeatureMapper = new PlanFeatureMapper();
        planMapper = new PlanMapper(planFeatureMapper);
        planService = new PlanServiceImpl(planRepository, planFeatureRepository, planMapper, planFeatureMapper);
    }

    /**
     * Property 8: Plan Deletion Protection
     * For any plan đang được sử dụng bởi ít nhất 1 company,
     * delete operation MUST trả về error và plan MUST không bị xóa.
     * 
     * Validates: Requirements 1.8
     */
    @Property(tries = 100)
    void planInUseShouldNotBeDeleted(
            @ForAll("validPlanIds") Long planId,
            @ForAll("positiveCompanyCounts") Long companyCount) {
        // Arrange: Plan tồn tại và đang được sử dụng bởi ít nhất 1 company
        PlanEntity existingPlan = createPlanEntity(planId);

        when(planRepository.findByIdAndDeletedFalse(planId))
                .thenReturn(Optional.of(existingPlan));
        when(planRepository.countCompaniesUsingPlan(planId))
                .thenReturn(companyCount);

        // Act & Assert: Delete phải throw BadRequestException
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> planService.delete(planId));

        // Verify: Plan không bị xóa (save không được gọi)
        verify(planRepository, never()).save(any(PlanEntity.class));

        // Verify: Error code đúng
        assertEquals("PLAN_IN_USE", exception.getErrorCode());
    }

    /**
     * Property: Plan không được sử dụng có thể xóa thành công
     * For any plan không được sử dụng bởi company nào,
     * delete operation MUST thành công và plan MUST bị soft delete.
     */
    @Property(tries = 100)
    void planNotInUseShouldBeDeleted(
            @ForAll("validPlanIds") Long planId) {
        // Arrange: Plan tồn tại và không được sử dụng
        PlanEntity existingPlan = createPlanEntity(planId);

        when(planRepository.findByIdAndDeletedFalse(planId))
                .thenReturn(Optional.of(existingPlan));
        when(planRepository.countCompaniesUsingPlan(planId))
                .thenReturn(0L);
        when(planFeatureRepository.findByPlanIdAndDeletedFalse(planId))
                .thenReturn(Collections.emptyList());
        when(planRepository.save(any(PlanEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Delete không throw exception
        assertDoesNotThrow(() -> planService.delete(planId));

        // Verify: Plan bị soft delete
        verify(planRepository).save(argThat(plan -> plan.getDeleted()));
    }

    /**
     * Property: Delete plan không tồn tại phải throw NotFoundException
     */
    @Property(tries = 100)
    void deleteNonExistentPlanShouldThrowNotFound(
            @ForAll("validPlanIds") Long planId) {
        // Arrange: Plan không tồn tại
        when(planRepository.findByIdAndDeletedFalse(planId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> planService.delete(planId));

        // Verify: Không có save nào được gọi
        verify(planRepository, never()).save(any(PlanEntity.class));
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> validPlanIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<Long> positiveCompanyCounts() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    // === Helper methods ===

    private PlanEntity createPlanEntity(Long id) {
        PlanEntity plan = new PlanEntity();
        plan.setId(id);
        plan.setNameVi("Gói cơ bản");
        plan.setNameEn("Basic Plan");
        plan.setNameJa("基本プラン");
        plan.setDescriptionVi("Mô tả gói cơ bản");
        plan.setDescriptionEn("Basic plan description");
        plan.setDescriptionJa("基本プランの説明");
        plan.setMonthlyPrice(BigDecimal.valueOf(100000));
        plan.setMaxEmployees(10);
        plan.setIsActive(true);
        plan.setDeleted(false);
        return plan;
    }
}
