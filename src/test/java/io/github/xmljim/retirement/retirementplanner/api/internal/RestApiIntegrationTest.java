/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * End-to-end coverage for the v1 REST surface (S-1.7). Boots the full
 * Spring Boot application against a Testcontainers Postgres, drives the
 * controllers through MockMvc, and verifies happy paths plus the 404
 * problem+json branches the AC requires.
 */
// Integration tests legitimately couple to the full controller surface area + Spring + Testcontainers + JPA bootstrap;
// MockMvc's chained .andExpect(...) calls ARE the assertions, but PMD's UnitTestShouldIncludeAssert can't recognize
// the fluent form; .throws Exception is the idiomatic shape for MockMvc's checked-exception API; static imports for
// MockMvc DSL are conventional and project-wide.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.SignatureDeclareThrowsException", "PMD.TooManyStaticImports"
})
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class RestApiIntegrationTest {

    private static final long SOLO_TENANT = TenantContext.SOLO_TENANT_ID;

    private static final String FILING_SINGLE = "SINGLE";
    private static final String FILING_MFJ = "MARRIED_FILING_JOINTLY";
    private static final String DOB_DEFAULT = "1975-06-15";
    private static final String STATE_VA = "VA";
    private static final String STATE_CA = "CA";

    private static final String PATH_PLANS = "/api/v1/plans";
    private static final String PATH_PLAN_BY_ID = "/api/v1/plans/{id}";
    private static final String PATH_PLAN_PROJECTION = "/api/v1/plans/{id}/projection";
    private static final String PATH_PLAN_PERSONS = "/api/v1/plans/{planId}/persons";
    private static final String PATH_PERSON_BY_ID = "/api/v1/persons/{id}";
    private static final String PATH_PLAN_ACCOUNTS = "/api/v1/plans/{planId}/accounts";
    private static final String PATH_ACCOUNT_BY_ID = "/api/v1/accounts/{id}";

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DOB = "dob";

    @Container
    @ServiceConnection
    @SuppressWarnings("PMD.MutableStaticState") // Testcontainers requires @Container fields to be static
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("retirement_planner")
            .withUsername("retirement")
            .withPassword("retirement");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void cleanState() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("DELETE FROM AccountSleeveEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM AccountEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM PersonEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM HouseholdEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM PlanEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM SalaryProfileEntity").executeUpdate();
        });
    }

    @Test
    @DisplayName("POST /api/v1/plans creates and returns 201 with Location header")
    void createPlanReturns201() throws Exception {
        ObjectNode body = newPlanRequest(FILING_SINGLE, STATE_VA, DOB_DEFAULT);
        mockMvc.perform(post(PATH_PLANS).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tenantId").value((int) SOLO_TENANT))
                .andExpect(jsonPath("$.household.state").value(STATE_VA))
                .andExpect(jsonPath("$.persons.length()").value(1))
                .andExpect(jsonPath("$.persons[0].id").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/plans/{id} on missing id returns 404 problem+json")
    void getMissingPlanReturns404() throws Exception {
        mockMvc.perform(get(PATH_PLAN_BY_ID, 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("9999")));
    }

    @Test
    @DisplayName("POST /api/v1/plans with invalid body returns 400 with field-level errors")
    void invalidPlanReturns400WithFieldErrors() throws Exception {
        ObjectNode body = json.createObjectNode();
        body.set("household", json.createObjectNode().put("state", "v"));
        body.set("persons", json.createArrayNode());
        mockMvc.perform(post(PATH_PLANS).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("PUT /api/v1/plans/{id} replaces household and 404s on missing")
    void putReplacesPlan() throws Exception {
        long planId = createPlan(FILING_SINGLE, STATE_VA, DOB_DEFAULT);
        ObjectNode update = newPlanRequest(FILING_MFJ, STATE_CA, DOB_DEFAULT);

        mockMvc.perform(put(PATH_PLAN_BY_ID, planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.household.filingStatus").value(FILING_MFJ))
                .andExpect(jsonPath("$.household.state").value(STATE_CA));

        mockMvc.perform(put(PATH_PLAN_BY_ID, 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/plans/{id}/projection returns deterministic month-by-month projection")
    void planProjectionEndpoint() throws Exception {
        long planId = createPlan(FILING_SINGLE, STATE_VA, DOB_DEFAULT);

        mockMvc.perform(get(PATH_PLAN_PROJECTION, planId).param("mode", "deterministic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].phase").value("ACCUMULATION"))
                .andExpect(jsonPath("$[0].period").exists())
                .andExpect(jsonPath("$[0].accountBalances").isArray())
                .andExpect(jsonPath("$[0].cashFlows").isArray());

        mockMvc.perform(get(PATH_PLAN_PROJECTION, 9999L).param("mode", "deterministic"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/plans/{id} returns 204 and subsequent GET returns 404")
    void deletePlanReturns204() throws Exception {
        long planId = createPlan(FILING_SINGLE, STATE_VA, DOB_DEFAULT);
        mockMvc.perform(delete(PATH_PLAN_BY_ID, planId)).andExpect(status().isNoContent());
        mockMvc.perform(get(PATH_PLAN_BY_ID, planId)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Person endpoints: add, fetch, replace, delete; 400 when adding a 3rd")
    void personEndpoints() throws Exception {
        long planId = createPlan(FILING_MFJ, STATE_CA, DOB_DEFAULT);

        ObjectNode addPerson =
                personJson("1978-04-22", "2045-01-01").putNull("id").putNull("salaryProfileId");
        MvcResult addedResult = mockMvc.perform(post(PATH_PLAN_PERSONS, planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addPerson.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.dob").value("1978-04-22"))
                .andReturn();
        long personId = json.readTree(addedResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(get(PATH_PERSON_BY_ID, personId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dob").value("1978-04-22"));

        ObjectNode replace = personJson("1979-01-01", "2045-01-01");
        mockMvc.perform(put(PATH_PERSON_BY_ID, personId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replace.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dob").value("1979-01-01"));

        ObjectNode third = personJson("1980-01-01", "2045-01-01");
        mockMvc.perform(post(PATH_PLAN_PERSONS, planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(third.toString()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete(PATH_PERSON_BY_ID, personId)).andExpect(status().isNoContent());
        mockMvc.perform(get(PATH_PERSON_BY_ID, personId)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Account create + sleeves round-trip with Money DTO")
    void accountCreateAndSleeves() throws Exception {
        long planId = createPlan(FILING_SINGLE, STATE_VA, DOB_DEFAULT);
        long personId = listFirstPersonId(planId);

        ObjectNode account = newAccountRequest(personId, "TRADITIONAL_IRA");
        MvcResult created = mockMvc.perform(post(PATH_PLAN_ACCOUNTS, planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(account.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("TRADITIONAL_IRA"))
                .andExpect(jsonPath("$.owner.type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.sleeves[0].balance.amount").value("50000.000000"))
                .andExpect(jsonPath("$.sleeves[0].balance.currency").value("USD"))
                .andReturn();
        long accountId = json.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(get("/api/v1/accounts/{id}/sleeves", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kind.type").value("ASSET_ALLOCATION"))
                .andExpect(jsonPath("$[0].yieldPolicy.type").value("TRACKS_ALLOCATION"));

        mockMvc.perform(get(PATH_PLAN_ACCOUNTS, planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) accountId));

        mockMvc.perform(get(PATH_ACCOUNT_BY_ID, 9999L)).andExpect(status().isNotFound());
        mockMvc.perform(delete(PATH_ACCOUNT_BY_ID, accountId)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Account replace swaps owner to JOINT and sleeves wholesale")
    void accountReplaceSwapsOwnerAndSleeves() throws Exception {
        long planId = createPlan(FILING_MFJ, STATE_CA, DOB_DEFAULT);
        long personId = listFirstPersonId(planId);

        ObjectNode initial = newAccountRequest(personId, "ROTH_IRA");
        MvcResult created = mockMvc.perform(post(PATH_PLAN_ACCOUNTS, planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initial.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        long accountId = json.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();

        ObjectNode replacement = json.createObjectNode();
        replacement.put(FIELD_TYPE, "TAXABLE_BROKERAGE");
        replacement.set("owner", json.createObjectNode().put(FIELD_TYPE, "JOINT"));
        ArrayNode sleeves = json.createArrayNode();
        ObjectNode cashSleeve = json.createObjectNode();
        cashSleeve.set("kind", json.createObjectNode().put(FIELD_TYPE, "CASH"));
        cashSleeve.set(
                "balance", json.createObjectNode().put("amount", "75000.00").put("currency", "USD"));
        cashSleeve.set(
                "yieldPolicy",
                json.createObjectNode().put(FIELD_TYPE, "FIXED_RATE").put("annualRate", "0.045"));
        sleeves.add(cashSleeve);
        replacement.set("sleeves", sleeves);

        mockMvc.perform(put(PATH_ACCOUNT_BY_ID, accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replacement.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TAXABLE_BROKERAGE"))
                .andExpect(jsonPath("$.owner.type").value("JOINT"))
                .andExpect(jsonPath("$.sleeves[0].kind.type").value("CASH"))
                .andExpect(jsonPath("$.sleeves[0].yieldPolicy.type").value("FIXED_RATE"));
    }

    private long createPlan(String filingStatus, String state, String dob) throws Exception {
        ObjectNode body = newPlanRequest(filingStatus, state, dob);
        MvcResult res = mockMvc.perform(
                        post(PATH_PLANS).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private long listFirstPersonId(long planId) throws Exception {
        MvcResult res = mockMvc.perform(get(PATH_PLAN_PERSONS, planId))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asLong();
    }

    private ObjectNode newPlanRequest(String filingStatus, String state, String dob) {
        ObjectNode plan = json.createObjectNode();
        ObjectNode household = json.createObjectNode();
        household.put("filingStatus", filingStatus);
        household.put("state", state);
        plan.set("household", household);
        ArrayNode persons = json.createArrayNode();
        persons.add(personJson(dob, "2040-01-01"));
        plan.set("persons", persons);
        ObjectNode assumptions = json.createObjectNode();
        assumptions.put("preRetirementReturnRate", "0.07");
        assumptions.put("cashInterestRate", "0.04");
        plan.set("assumptions", assumptions);
        return plan;
    }

    private ObjectNode personJson(String dob, String retirementDate) {
        return json.createObjectNode().put(FIELD_DOB, dob).put("retirementDate", retirementDate);
    }

    private ObjectNode newAccountRequest(long ownerPersonId, String accountType) {
        ObjectNode account = json.createObjectNode();
        account.put(FIELD_TYPE, accountType);
        ObjectNode owner = json.createObjectNode();
        owner.put(FIELD_TYPE, "INDIVIDUAL");
        owner.put("personId", ownerPersonId);
        account.set("owner", owner);
        ArrayNode sleeves = json.createArrayNode();
        ObjectNode sleeve = json.createObjectNode();
        sleeve.set("kind", json.createObjectNode().put(FIELD_TYPE, "ASSET_ALLOCATION"));
        sleeve.set("balance", json.createObjectNode().put("amount", "50000.00").put("currency", "USD"));
        sleeve.set("yieldPolicy", json.createObjectNode().put(FIELD_TYPE, "TRACKS_ALLOCATION"));
        sleeves.add(sleeve);
        account.set("sleeves", sleeves);
        return account;
    }
}
