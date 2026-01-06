package com.tamabee.api_hr.service;

import com.tamabee.api_hr.dto.response.PlanFeaturesResponse;
import com.tamabee.api_hr.dto.response.PlanFeaturesResponse.FeatureItem;
import com.tamabee.api_hr.entity.wallet.PlanEntity;
import com.tamabee.api_hr.entity.wallet.PlanFeatureCodeEntity;
import com.tamabee.api_hr.enums.FeatureCode;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.repository.PlanFeatureCodeRepository;
import com.tamabee.api_hr.repository.PlanRepository;
import com.tamabee.api_hr.service.core.impl.PlanFeaturesServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Plan Features API.
 * 
 * Property 9: Plan Features API Response
 * - For any valid planId, API SHALL return all features associated with that
 * plan
 * - For any request without planId (Tamabee users), API SHALL return all
 * features with enabled = true
 */
class PlanFeaturesPropertyTest {

    // ==================== Property 9.1: Features by PlanId ====================

    /**
     * Property 9.1.1: Response phải chứa planId và planName
     * For any valid plan, response SHALL contain planId and planName
     */
    @Property(tries = 100)
    void getFeaturesByPlanId_shouldContainPlanIdAndName(
            @ForAll @LongRange(min = 1, max = 1000) Long planId,
            @ForAll("validPlanNames") String planName) {

        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        PlanEntity plan = createPlanEntity(planId, planName);
        when(planRepository.findByIdAndDeletedFalse(planId)).thenReturn(Optional.of(plan));
        when(planFeatureCodeRepository.findByPlanIdAndDeletedFalse(planId)).thenReturn(List.of());

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute
        PlanFeaturesResponse response = service.getFeaturesByPlanId(planId);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getPlanId()).isEqualTo(planId);
        assertThat(response.getPlanName()).isNotNull();
        assertThat(response.getPlanName()).isNotEmpty();
    }

    /**
     * Property 9.1.2: Response phải chứa tất cả FeatureCode values
     * For any valid plan, response SHALL contain ALL FeatureCode enum values
     */
    @Property(tries = 100)
    void getFeaturesByPlanId_shouldContainAllFeatureCodes(
            @ForAll @LongRange(min = 1, max = 1000) Long planId,
            @ForAll("validPlanNames") String planName,
            @ForAll("featureCodeSubsets") Set<FeatureCode> enabledFeatures) {

        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        PlanEntity plan = createPlanEntity(planId, planName);
        List<PlanFeatureCodeEntity> featureEntities = enabledFeatures.stream()
                .map(code -> createPlanFeatureCodeEntity(planId, code))
                .collect(Collectors.toList());

        when(planRepository.findByIdAndDeletedFalse(planId)).thenReturn(Optional.of(plan));
        when(planFeatureCodeRepository.findByPlanIdAndDeletedFalse(planId)).thenReturn(featureEntities);

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute
        PlanFeaturesResponse response = service.getFeaturesByPlanId(planId);

        // Verify - response contains ALL FeatureCode values
        assertThat(response.getFeatures()).isNotNull();
        Set<FeatureCode> responseCodes = response.getFeatures().stream()
                .map(FeatureItem::getCode)
                .collect(Collectors.toSet());
        assertThat(responseCodes).containsExactlyInAnyOrderElementsOf(
                Arrays.asList(FeatureCode.values()));
    }

    /**
     * Property 9.1.3: Enabled features phải match với plan's feature codes
     * For any plan with specific features, only those features SHALL be enabled
     */
    @Property(tries = 100)
    void getFeaturesByPlanId_enabledFeatures_shouldMatchPlanFeatureCodes(
            @ForAll @LongRange(min = 1, max = 1000) Long planId,
            @ForAll("validPlanNames") String planName,
            @ForAll("featureCodeSubsets") Set<FeatureCode> enabledFeatures) {

        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        PlanEntity plan = createPlanEntity(planId, planName);
        List<PlanFeatureCodeEntity> featureEntities = enabledFeatures.stream()
                .map(code -> createPlanFeatureCodeEntity(planId, code))
                .collect(Collectors.toList());

        when(planRepository.findByIdAndDeletedFalse(planId)).thenReturn(Optional.of(plan));
        when(planFeatureCodeRepository.findByPlanIdAndDeletedFalse(planId)).thenReturn(featureEntities);

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute
        PlanFeaturesResponse response = service.getFeaturesByPlanId(planId);

        // Verify - enabled features match exactly
        Set<FeatureCode> responseEnabledCodes = response.getFeatures().stream()
                .filter(FeatureItem::isEnabled)
                .map(FeatureItem::getCode)
                .collect(Collectors.toSet());
        assertThat(responseEnabledCodes).isEqualTo(enabledFeatures);

        // Verify - disabled features are the complement
        Set<FeatureCode> responseDisabledCodes = response.getFeatures().stream()
                .filter(f -> !f.isEnabled())
                .map(FeatureItem::getCode)
                .collect(Collectors.toSet());
        Set<FeatureCode> expectedDisabled = Arrays.stream(FeatureCode.values())
                .filter(code -> !enabledFeatures.contains(code))
                .collect(Collectors.toSet());
        assertThat(responseDisabledCodes).isEqualTo(expectedDisabled);
    }

    /**
     * Property 9.1.4: Mỗi feature phải có name không rỗng
     * For any feature in response, name SHALL NOT be empty
     */
    @Property(tries = 100)
    void getFeaturesByPlanId_allFeatures_shouldHaveNonEmptyName(
            @ForAll @LongRange(min = 1, max = 1000) Long planId,
            @ForAll("validPlanNames") String planName) {

        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        PlanEntity plan = createPlanEntity(planId, planName);
        when(planRepository.findByIdAndDeletedFalse(planId)).thenReturn(Optional.of(plan));
        when(planFeatureCodeRepository.findByPlanIdAndDeletedFalse(planId)).thenReturn(List.of());

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute
        PlanFeaturesResponse response = service.getFeaturesByPlanId(planId);

        // Verify - all features have non-empty names
        for (FeatureItem feature : response.getFeatures()) {
            assertThat(feature.getName())
                    .as("Feature %s should have non-empty name", feature.getCode())
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    /**
     * Property 9.1.5: Plan không tồn tại phải throw NotFoundException
     * For any non-existent planId, service SHALL throw NotFoundException
     */
    @Property(tries = 50)
    void getFeaturesByPlanId_nonExistentPlan_shouldThrowNotFoundException(
            @ForAll @LongRange(min = 1, max = 1000) Long planId) {

        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        when(planRepository.findByIdAndDeletedFalse(planId)).thenReturn(Optional.empty());

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute & Verify
        assertThatThrownBy(() -> service.getFeaturesByPlanId(planId))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== Property 9.2: All Features Enabled (Tamabee)
    // ====================

    /**
     * Property 9.2.1: Tamabee response phải có planId = null
     * For Tamabee users, response SHALL have planId = null
     */
    @Property(tries = 50)
    void getAllFeaturesEnabled_shouldHaveNullPlanId() {
        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute
        PlanFeaturesResponse response = service.getAllFeaturesEnabled();

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getPlanId()).isNull();
    }

    /**
     * Property 9.2.2: Tamabee response phải có planName = "Tamabee"
     * For Tamabee users, response SHALL have planName = "Tamabee"
     */
    @Property(tries = 50)
    void getAllFeaturesEnabled_shouldHaveTamabeePlanName() {
        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute
        PlanFeaturesResponse response = service.getAllFeaturesEnabled();

        // Verify
        assertThat(response.getPlanName()).isEqualTo("Tamabee");
    }

    /**
     * Property 9.2.3: Tamabee response phải có tất cả features enabled
     * For Tamabee users, ALL features SHALL be enabled
     */
    @Property(tries = 50)
    void getAllFeaturesEnabled_allFeatures_shouldBeEnabled() {
        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute
        PlanFeaturesResponse response = service.getAllFeaturesEnabled();

        // Verify - all features are enabled
        assertThat(response.getFeatures()).isNotNull();
        assertThat(response.getFeatures()).hasSize(FeatureCode.values().length);

        for (FeatureItem feature : response.getFeatures()) {
            assertThat(feature.isEnabled())
                    .as("Feature %s should be enabled for Tamabee", feature.getCode())
                    .isTrue();
        }
    }

    /**
     * Property 9.2.4: Tamabee response phải chứa tất cả FeatureCode values
     * For Tamabee users, response SHALL contain ALL FeatureCode enum values
     */
    @Property(tries = 50)
    void getAllFeaturesEnabled_shouldContainAllFeatureCodes() {
        // Setup mocks
        PlanRepository planRepository = mock(PlanRepository.class);
        PlanFeatureCodeRepository planFeatureCodeRepository = mock(PlanFeatureCodeRepository.class);

        PlanFeaturesServiceImpl service = new PlanFeaturesServiceImpl(planRepository, planFeatureCodeRepository);

        // Execute
        PlanFeaturesResponse response = service.getAllFeaturesEnabled();

        // Verify
        Set<FeatureCode> responseCodes = response.getFeatures().stream()
                .map(FeatureItem::getCode)
                .collect(Collectors.toSet());
        assertThat(responseCodes).containsExactlyInAnyOrderElementsOf(
                Arrays.asList(FeatureCode.values()));
    }

    // ==================== Providers ====================

    /**
     * Provider cho valid plan names
     */
    @Provide
    Arbitrary<String> validPlanNames() {
        return Arbitraries.of("Basic", "Standard", "Premium", "Enterprise", "Starter", "Pro");
    }

    /**
     * Provider cho subsets of FeatureCode
     */
    @Provide
    Arbitrary<Set<FeatureCode>> featureCodeSubsets() {
        return Arbitraries.of(FeatureCode.values())
                .set()
                .ofMinSize(0)
                .ofMaxSize(FeatureCode.values().length);
    }

    // ==================== Helper Methods ====================

    private PlanEntity createPlanEntity(Long id, String name) {
        PlanEntity plan = new PlanEntity();
        plan.setId(id);
        plan.setNameVi(name + " VI");
        plan.setNameEn(name + " EN");
        plan.setNameJa(name + " JA");
        plan.setDescriptionVi("Description VI");
        plan.setDescriptionEn("Description EN");
        plan.setDescriptionJa("Description JA");
        plan.setMonthlyPrice(BigDecimal.valueOf(100));
        plan.setMaxEmployees(50);
        plan.setIsActive(true);
        plan.setDeleted(false);
        return plan;
    }

    private PlanFeatureCodeEntity createPlanFeatureCodeEntity(Long planId, FeatureCode code) {
        PlanFeatureCodeEntity entity = new PlanFeatureCodeEntity();
        entity.setPlanId(planId);
        entity.setFeatureCode(code);
        entity.setDeleted(false);
        return entity;
    }
}
