/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.internal;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Pins the OpenAPI document's {@code info} and {@code servers} so the
 * exported {@code docs/api/openapi.yaml} is byte-stable across builds.
 * Without this, springdoc fills {@code servers[0].url} with the random
 * Testcontainers port and the spec churns every run.
 */
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI retirementPlannerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Retirement Planner API")
                        .description("REST surface for the retirement planner backend.")
                        .version("v0"))
                .servers(List.of(new Server().url("/").description("Relative to the deployment host")));
    }
}
