/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.xmljim.retirement.retirementplanner.api.dto.PlanDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * REST contract for {@link io.github.xmljim.retirement.retirementplanner.plan.Plan}
 * resources. Implemented by a {@code @RestController} in
 * {@code api/internal} so the contract surface stays decoupled from the
 * delegation logic.
 */
@Tag(name = "Plans", description = "Top-level retirement plan aggregates")
@RequestMapping(path = "/api/v1/plans", produces = MediaType.APPLICATION_JSON_VALUE)
public interface PlanOperations {

    @Operation(summary = "Create a Plan in the active tenant")
    @ApiResponse(responseCode = "201", description = "Plan created")
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PlanDto> create(@Valid @RequestBody PlanDto plan);

    @Operation(summary = "Fetch a Plan by id")
    @ApiResponse(responseCode = "200", description = "Plan found")
    @ApiResponse(
            responseCode = "404",
            description = "Plan not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    PlanDto findById(@PathVariable("id") long id);

    @Operation(summary = "List all Plans visible in the active tenant")
    @ApiResponse(responseCode = "200", description = "List of plans (possibly empty)")
    @GetMapping
    List<PlanDto> findAll();

    @Operation(summary = "Replace a Plan's household scalars")
    @ApiResponse(responseCode = "200", description = "Plan replaced")
    @ApiResponse(
            responseCode = "404",
            description = "Plan not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    PlanDto replace(@PathVariable("id") long id, @Valid @RequestBody PlanDto plan);

    @Operation(summary = "Delete a Plan by id (cascades to Persons and Accounts)")
    @ApiResponse(responseCode = "204", description = "Deleted (or absent)")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteById(@PathVariable("id") long id);

    /** Helper for the impl to build a 201 with Location. */
    static URI locationOf(long id) {
        return URI.create("/api/v1/plans/" + id);
    }
}
