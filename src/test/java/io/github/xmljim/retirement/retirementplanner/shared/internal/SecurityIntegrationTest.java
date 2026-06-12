/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end coverage for the security filter chain wired in S-1.8:
 * stub authentication, CORS preflight against the configured allowlist,
 * and {@link OriginCheckFilter} enforcement on state-changing methods.
 */
// MockMvc fluent .andExpect(...) is the assertion DSL; PMD doesn't recognize the form.
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.cors.allowed-origins=https://app.example.com"})
class SecurityIntegrationTest {

    private static final String ALLOWED_ORIGIN = "https://app.example.com";
    private static final String BLOCKED_ORIGIN = "https://evil.example.com";
    private static final String PATH_PLANS = "/api/v1/plans";

    @Container
    @ServiceConnection
    @SuppressWarnings("PMD.MutableStaticState") // Testcontainers requires @Container fields to be static
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("retirement_planner")
            .withUsername("retirement")
            .withPassword("retirement");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("CORS preflight from allowed origin returns matching Access-Control-Allow-Origin")
    void preflightFromAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options(PATH_PLANS)
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("CORS preflight from disallowed origin is rejected")
    void preflightFromDisallowedOriginIsRejected() throws Exception {
        mockMvc.perform(options(PATH_PLANS)
                        .header(HttpHeaders.ORIGIN, BLOCKED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST from disallowed origin is blocked by OriginCheckFilter with 403")
    void postFromDisallowedOriginIsForbidden() throws Exception {
        mockMvc.perform(post(PATH_PLANS)
                        .header(HttpHeaders.ORIGIN, BLOCKED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST from non-browser client (no Origin header) reaches the controller")
    void postWithoutOriginReachesController() throws Exception {
        mockMvc.perform(post(PATH_PLANS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                // 400 = controller reached and bean validation rejected the empty body — proof the
                // filter chain let it through (auth + origin).
                .andExpect(status().isBadRequest());
    }
}
