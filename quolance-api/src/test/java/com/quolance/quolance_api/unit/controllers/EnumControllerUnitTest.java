package com.quolance.quolance_api.unit.controllers;

import com.quolance.quolance_api.controllers.EnumController;
import com.quolance.quolance_api.entities.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnumControllerUnitTest {

    private static final List<String> EXPECTED_PROJECT_CATEGORIES = Arrays.stream(ProjectCategory.values())
            .map(Enum::name)
            .toList();
    private static final List<String> EXPECTED_APPLICATION_STATUSES = Arrays.stream(ApplicationStatus.values())
            .map(Enum::name)
            .toList();
    private static final List<String> EXPECTED_DELIVERY_TIMES = Arrays.stream(ExpectedDeliveryTime.values())
            .map(Enum::name)
            .toList();
    private static final List<String> EXPECTED_EXPERIENCE_LEVELS = Arrays.stream(FreelancerExperienceLevel.values())
            .map(Enum::name)
            .toList();
    private static final List<String> EXPECTED_PRICE_RANGES = Arrays.stream(PriceRange.values())
            .map(Enum::name)
            .toList();
    private static final List<String> EXPECTED_PROJECT_STATUSES = Arrays.stream(ProjectStatus.values())
            .map(Enum::name)
            .toList();
    private static final List<String> EXPECTED_ROLES = Arrays.stream(Role.values())
            .map(Enum::name)
            .toList();
    private static final List<String> EXPECTED_TAGS = Arrays.stream(Tag.values())
            .map(Enum::name)
            .toList();

    private EnumController enumController;

    @BeforeEach
    void setUp() {
        enumController = new EnumController();
    }

    @Test
    void getProjectCategories_ShouldReturnAllCategories() {
        ResponseEntity<List<String>> response = enumController.getProjectCategories();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_PROJECT_CATEGORIES);
        assertResponseFormat(response);
    }

    @Test
    void getApplicationStatuses_ShouldReturnAllStatuses() {
        ResponseEntity<List<String>> response = enumController.getApplicationStatuses();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_APPLICATION_STATUSES);
        assertResponseFormat(response);
    }

    @Test
    void getExpectedDeliveryTimes_ShouldReturnAllDeliveryTimes() {
        ResponseEntity<List<String>> response = enumController.getExpectedDeliveryTimes();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_DELIVERY_TIMES);
        assertResponseFormat(response);
    }

    @Test
    void getFreelancerExperienceLevels_ShouldReturnAllExperienceLevels() {
        ResponseEntity<List<String>> response = enumController.getFreelancerExperienceLevels();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_EXPERIENCE_LEVELS);
        assertResponseFormat(response);
    }

    @Test
    void getPriceRanges_ShouldReturnAllPriceRanges() {
        ResponseEntity<List<String>> response = enumController.getPriceRanges();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_PRICE_RANGES);
        assertResponseFormat(response);
    }

    @Test
    void getProjectStatuses_ShouldReturnAllProjectStatuses() {
        ResponseEntity<List<String>> response = enumController.getProjectStatuses();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_PROJECT_STATUSES);
        assertResponseFormat(response);
    }

    @Test
    void getRoles_ShouldReturnAllRoles() {
        ResponseEntity<List<String>> response = enumController.getRoles();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_ROLES);
        assertResponseFormat(response);
    }

    @Test
    void getTags_ShouldReturnAllTags() {
        ResponseEntity<List<String>> response = enumController.getTags();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EXPECTED_TAGS);
        assertResponseFormat(response);
    }

    @Test
    void allEndpoints_ShouldReturnNonEmptyLists() {
        List<ResponseEntity<List<String>>> responses = List.of(
                enumController.getProjectCategories(),
                enumController.getApplicationStatuses(),
                enumController.getExpectedDeliveryTimes(),
                enumController.getFreelancerExperienceLevels(),
                enumController.getPriceRanges(),
                enumController.getProjectStatuses(),
                enumController.getRoles(),
                enumController.getTags()
        );

        responses.forEach(response -> {
            assertThat(response.getBody())
                    .isNotNull()
                    .isNotEmpty();
        });
    }

    @Test
    void allEndpoints_ShouldHaveConsistentResponseFormat() {
        List<ResponseEntity<List<String>>> responses = List.of(
                enumController.getProjectCategories(),
                enumController.getApplicationStatuses(),
                enumController.getExpectedDeliveryTimes(),
                enumController.getFreelancerExperienceLevels(),
                enumController.getPriceRanges(),
                enumController.getProjectStatuses(),
                enumController.getRoles(),
                enumController.getTags()
        );

        responses.forEach(this::assertResponseFormat);
    }

    private void assertResponseFormat(ResponseEntity<List<String>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isInstanceOf(List.class);
        assertThat(response.getBody().stream().allMatch(String.class::isInstance)).isTrue();
    }
}