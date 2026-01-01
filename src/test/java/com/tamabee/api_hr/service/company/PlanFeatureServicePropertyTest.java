package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.response.PlanFeaturesResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.PlanFeatureCodeEntity;
import com.tamabee.api_hr.enums.FeatureCode;
import com.tamabee.api_hr.exception.ForbiddenException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.repository.PlanFeatureCodeRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.service.company.impl.PlanFeatureServiceImpl;
import net.jqwik.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho PlanFeatureService
 * Feature: attendance-payroll-backend, Property 24: Plan Feature Access Control
 */
public class PlanFeatureServicePropertyTest {

    /**
     * Property 24: Plan Feature Access Control
     * For any feature access request, the system SHALL allow access only if
     * the company's plan includes that feature.
     */
    @Property(tries = 100)
    void hasFeatureAccessReturnsTrueOnlyWhenPlanIncludesFeature(
            @ForAll("companyIds") Long companyId,
            @ForAll("planIds") Long planId,
            @ForAll("featureCodes") FeatureCode requestedFeature,
            @ForAll("featureCodeSets") Set<FeatureCode> planFeatures) {

        // Arrange
        CompanyRepository mockCompanyRepo = mock(CompanyRepository.class);
        PlanRepository mockPlanRepo = mock(PlanRepository.class);
        PlanFeatureCodeRepository mockFeatureRepo = mock(PlanFeatureCodeRepository.class);

        // Setup company với planId
        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);
        company.setPlanId(planId);
        when(mockCompanyRepo.findById(companyId)).thenReturn(Optional.of(company));

        // Setup feature check - trả về true nếu feature nằm trong planFeatures
        boolean featureInPlan = planFeatures.contains(requestedFeature);
        when(mockFeatureRepo.existsByPlanIdAndFeatureCodeAndDeletedFalse(planId, requestedFeature))
                .thenReturn(featureInPlan);

        PlanFeatureServiceImpl service = new PlanFeatureServiceImpl(
                mockCompanyRepo, mockPlanRepo, mockFeatureRepo);

        // Act
        boolean hasAccess = service.hasFeatureAccess(companyId, requestedFeature);

        // Assert - Access should match whether feature is in plan
        assertEquals(featureInPlan, hasAccess,
                String.format("hasFeatureAccess should return %s when feature %s is %s in plan features %s",
                        featureInPlan, requestedFeature,
                        featureInPlan ? "included" : "not included",
                        planFeatures));
    }

    /**
     * Property: validateFeatureAccess throws ForbiddenException when feature not in
     * plan
     */
    @Property(tries = 100)
    void validateFeatureAccessThrowsWhenFeatureNotInPlan(
            @ForAll("companyIds") Long companyId,
            @ForAll("planIds") Long planId,
            @ForAll("featureCodes") FeatureCode requestedFeature) {

        // Arrange
        CompanyRepository mockCompanyRepo = mock(CompanyRepository.class);
        PlanRepository mockPlanRepo = mock(PlanRepository.class);
        PlanFeatureCodeRepository mockFeatureRepo = mock(PlanFeatureCodeRepository.class);

        // Setup company với planId
        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);
        company.setPlanId(planId);
        when(mockCompanyRepo.findById(companyId)).thenReturn(Optional.of(company));

        // Feature không có trong plan
        when(mockFeatureRepo.existsByPlanIdAndFeatureCodeAndDeletedFalse(planId, requestedFeature))
                .thenReturn(false);

        PlanFeatureServiceImpl service = new PlanFeatureServiceImpl(
                mockCompanyRepo, mockPlanRepo, mockFeatureRepo);

        // Act & Assert
        assertThrows(ForbiddenException.class,
                () -> service.validateFeatureAccess(companyId, requestedFeature),
                "Should throw ForbiddenException when feature is not in plan");
    }

    /**
     * Property: validateFeatureAccess does not throw when feature is in plan
     */
    @Property(tries = 100)
    void validateFeatureAccessDoesNotThrowWhenFeatureInPlan(
            @ForAll("companyIds") Long companyId,
            @ForAll("planIds") Long planId,
            @ForAll("featureCodes") FeatureCode requestedFeature) {

        // Arrange
        CompanyRepository mockCompanyRepo = mock(CompanyRepository.class);
        PlanRepository mockPlanRepo = mock(PlanRepository.class);
        PlanFeatureCodeRepository mockFeatureRepo = mock(PlanFeatureCodeRepository.class);

        // Setup company với planId
        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);
        company.setPlanId(planId);
        when(mockCompanyRepo.findById(companyId)).thenReturn(Optional.of(company));

        // Feature có trong plan
        when(mockFeatureRepo.existsByPlanIdAndFeatureCodeAndDeletedFalse(planId, requestedFeature))
                .thenReturn(true);

        PlanFeatureServiceImpl service = new PlanFeatureServiceImpl(
                mockCompanyRepo, mockPlanRepo, mockFeatureRepo);

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> service.validateFeatureAccess(companyId, requestedFeature),
                "Should not throw when feature is in plan");
    }

    /**
     * Property: Company without plan has no feature access
     */
    @Property(tries = 100)
    void companyWithoutPlanHasNoFeatureAccess(
            @ForAll("companyIds") Long companyId,
            @ForAll("featureCodes") FeatureCode requestedFeature) {

        // Arrange
        CompanyRepository mockCompanyRepo = mock(CompanyRepository.class);
        PlanRepository mockPlanRepo = mock(PlanRepository.class);
        PlanFeatureCodeRepository mockFeatureRepo = mock(PlanFeatureCodeRepository.class);

        // Setup company không có planId
        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);
        company.setPlanId(null);
        when(mockCompanyRepo.findById(companyId)).thenReturn(Optional.of(company));

        PlanFeatureServiceImpl service = new PlanFeatureServiceImpl(
                mockCompanyRepo, mockPlanRepo, mockFeatureRepo);

        // Act
        boolean hasAccess = service.hasFeatureAccess(companyId, requestedFeature);

        // Assert
        assertFalse(hasAccess, "Company without plan should have no feature access");
    }

    /**
     * Property: getCompanyFeatures returns all features from plan
     */
    @Property(tries = 100)
    void getCompanyFeaturesReturnsAllPlanFeatures(
            @ForAll("companyIds") Long companyId,
            @ForAll("planIds") Long planId,
            @ForAll("featureCodeSets") Set<FeatureCode> planFeatures) {

        // Arrange
        CompanyRepository mockCompanyRepo = mock(CompanyRepository.class);
        PlanRepository mockPlanRepo = mock(PlanRepository.class);
        PlanFeatureCodeRepository mockFeatureRepo = mock(PlanFeatureCodeRepository.class);

        // Setup company với planId
        CompanyEntity company = new CompanyEntity();
        company.setId(companyId);
        company.setPlanId(planId);
        when(mockCompanyRepo.findById(companyId)).thenReturn(Optional.of(company));

        // Setup feature entities
        List<PlanFeatureCodeEntity> featureEntities = planFeatures.stream()
                .map(fc -> {
                    PlanFeatureCodeEntity entity = new PlanFeatureCodeEntity();
                    entity.setPlanId(planId);
                    entity.setFeatureCode(fc);
                    return entity;
                })
                .collect(Collectors.toList());
        when(mockFeatureRepo.findByPlanIdAndDeletedFalse(planId)).thenReturn(featureEntities);

        PlanFeatureServiceImpl service = new PlanFeatureServiceImpl(
                mockCompanyRepo, mockPlanRepo, mockFeatureRepo);

        // Act
        List<FeatureCode> result = service.getCompanyFeatures(companyId);

        // Assert
        assertEquals(planFeatures.size(), result.size(),
                "Should return same number of features as in plan");
        assertTrue(new HashSet<>(result).containsAll(planFeatures),
                "Should return all features from plan");
    }

    /**
     * Property: getPlanFeatures returns correct plan info and features
     */
    @Property(tries = 100)
    void getPlanFeaturesReturnsCorrectInfo(
            @ForAll("planIds") Long planId,
            @ForAll("planNames") String planName,
            @ForAll("featureCodeSets") Set<FeatureCode> planFeatures) {

        // Arrange
        CompanyRepository mockCompanyRepo = mock(CompanyRepository.class);
        PlanRepository mockPlanRepo = mock(PlanRepository.class);
        PlanFeatureCodeRepository mockFeatureRepo = mock(PlanFeatureCodeRepository.class);

        // Setup plan
        PlanEntity plan = new PlanEntity();
        plan.setId(planId);
        plan.setNameEn(planName);
        when(mockPlanRepo.findByIdAndDeletedFalse(planId)).thenReturn(Optional.of(plan));

        // Setup feature entities
        List<PlanFeatureCodeEntity> featureEntities = planFeatures.stream()
                .map(fc -> {
                    PlanFeatureCodeEntity entity = new PlanFeatureCodeEntity();
                    entity.setPlanId(planId);
                    entity.setFeatureCode(fc);
                    return entity;
                })
                .collect(Collectors.toList());
        when(mockFeatureRepo.findByPlanIdAndDeletedFalse(planId)).thenReturn(featureEntities);

        PlanFeatureServiceImpl service = new PlanFeatureServiceImpl(
                mockCompanyRepo, mockPlanRepo, mockFeatureRepo);

        // Act
        PlanFeaturesResponse result = service.getPlanFeatures(planId);

        // Assert
        assertEquals(planId, result.getPlanId(), "Plan ID should match");
        assertEquals(planName, result.getPlanName(), "Plan name should match");
        assertEquals(planFeatures, result.getFeatures(), "Features should match");
    }

    /**
     * Property: getPlanFeatures throws NotFoundException for non-existent plan
     */
    @Property(tries = 100)
    void getPlanFeaturesThrowsForNonExistentPlan(@ForAll("planIds") Long planId) {
        // Arrange
        CompanyRepository mockCompanyRepo = mock(CompanyRepository.class);
        PlanRepository mockPlanRepo = mock(PlanRepository.class);
        PlanFeatureCodeRepository mockFeatureRepo = mock(PlanFeatureCodeRepository.class);

        when(mockPlanRepo.findByIdAndDeletedFalse(planId)).thenReturn(Optional.empty());

        PlanFeatureServiceImpl service = new PlanFeatureServiceImpl(
                mockCompanyRepo, mockPlanRepo, mockFeatureRepo);

        // Act & Assert
        assertThrows(NotFoundException.class,
                () -> service.getPlanFeatures(planId),
                "Should throw NotFoundException for non-existent plan");
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> companyIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    @Provide
    Arbitrary<Long> planIds() {
        return Arbitraries.longs().between(1L, 100L);
    }

    @Provide
    Arbitrary<FeatureCode> featureCodes() {
        return Arbitraries.of(FeatureCode.values());
    }

    @Provide
    Arbitrary<Set<FeatureCode>> featureCodeSets() {
        return Arbitraries.of(FeatureCode.values())
                .set()
                .ofMinSize(0)
                .ofMaxSize(FeatureCode.values().length);
    }

    @Provide
    Arbitrary<String> planNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50);
    }
}
