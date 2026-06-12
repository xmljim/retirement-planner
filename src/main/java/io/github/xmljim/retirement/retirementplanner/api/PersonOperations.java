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

import io.github.xmljim.retirement.retirementplanner.api.dto.PersonDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * REST contract for {@link io.github.xmljim.retirement.retirementplanner.plan.Person}
 * resources. Persons are addressable directly by id once created; the
 * collection lives under the parent Plan.
 */
@Tag(name = "Persons", description = "Persons (spouses) within a Plan's Household")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public interface PersonOperations {

    @Operation(summary = "List all Persons in a Plan")
    @ApiResponse(responseCode = "200", description = "List of persons (1 or 2 entries)")
    @ApiResponse(
            responseCode = "404",
            description = "Plan not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/api/v1/plans/{planId}/persons")
    List<PersonDto> findByPlanId(@PathVariable("planId") long planId);

    @Operation(summary = "Add a Person to a Plan (max 2)")
    @ApiResponse(responseCode = "201", description = "Person created")
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed or Plan already has 2 persons",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(value = "/api/v1/plans/{planId}/persons", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PersonDto> add(@PathVariable("planId") long planId, @Valid @RequestBody PersonDto person);

    @Operation(summary = "Fetch a Person by id")
    @ApiResponse(responseCode = "200", description = "Person found")
    @ApiResponse(
            responseCode = "404",
            description = "Person not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/api/v1/persons/{id}")
    PersonDto findById(@PathVariable("id") long id);

    @Operation(summary = "Replace a Person's scalars (e.g. dob)")
    @ApiResponse(responseCode = "200", description = "Person replaced")
    @ApiResponse(
            responseCode = "404",
            description = "Person not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(value = "/api/v1/persons/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    PersonDto replace(@PathVariable("id") long id, @Valid @RequestBody PersonDto person);

    @Operation(summary = "Delete a Person by id (refuses to remove the last Person from a Plan)")
    @ApiResponse(responseCode = "204", description = "Deleted (or absent)")
    @ApiResponse(
            responseCode = "400",
            description = "Cannot delete the last Person from a Plan",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/api/v1/persons/{id}")
    ResponseEntity<Void> deleteById(@PathVariable("id") long id);

    static URI locationOf(long id) {
        return URI.create("/api/v1/persons/" + id);
    }
}
