/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the application against a Testcontainers Postgres, fetches the
 * springdoc-generated OpenAPI YAML, and writes it to
 * {@code docs/api/openapi.yaml} on every build. CI's existing
 * "no uncommitted changes after build" check catches drift.
 *
 * <p>The spec covers the controller surface defined by the {@code
 * *Operations} interfaces in {@code api/}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiSpecExportIntegrationTest {

    private static final Path SPEC_PATH = Paths.get("docs", "api", "openapi.yaml");

    @Container
    @ServiceConnection
    @SuppressWarnings("PMD.MutableStaticState") // Testcontainers requires @Container fields to be static
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("retirement_planner")
            .withUsername("retirement")
            .withPassword("retirement");

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("exports OpenAPI YAML to docs/api/openapi.yaml")
    void exportsOpenApiYaml() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:" + port + "/v3/api-docs.yaml"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String generated = response.body();
        assertThat(response.statusCode()).as("springdoc must serve the spec").isEqualTo(200);

        assertThat(generated)
                .as("springdoc must serve a non-empty spec covering the v1 endpoints")
                .isNotNull()
                .contains("/api/v1/plans")
                .contains("/api/v1/accounts");

        Files.createDirectories(SPEC_PATH.getParent());
        Files.writeString(SPEC_PATH, generated, StandardCharsets.UTF_8);
    }
}
