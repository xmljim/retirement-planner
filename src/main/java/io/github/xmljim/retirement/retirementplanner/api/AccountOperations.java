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

import io.github.xmljim.retirement.retirementplanner.api.dto.AccountDto;
import io.github.xmljim.retirement.retirementplanner.api.dto.AccountSleeveDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * REST contract for {@link io.github.xmljim.retirement.retirementplanner.plan.Account}
 * resources. Accounts are addressable directly by id once created;
 * the collection lives under the parent Plan. Sleeves are a read-only
 * sub-resource — they're created/replaced by replacing the parent
 * Account.
 */
// HTTP response codes ("200", "404") repeat across @ApiResponse annotations on each operation; that's the documented
// shape of springdoc's contract. Extracting them to constants would obscure the spec, not improve it.
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Tag(name = "Accounts", description = "Tax-advantaged or taxable accounts within a Plan")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public interface AccountOperations {

    @Operation(summary = "List all Accounts in a Plan")
    @ApiResponse(responseCode = "200", description = "List of accounts (possibly empty)")
    @GetMapping("/api/v1/plans/{planId}/accounts")
    List<AccountDto> findByPlanId(@PathVariable("planId") long planId);

    @Operation(summary = "Create an Account under a Plan")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed or parent Plan not in active tenant",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(value = "/api/v1/plans/{planId}/accounts", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AccountDto> create(@PathVariable("planId") long planId, @Valid @RequestBody AccountDto account);

    @Operation(summary = "Fetch an Account by id")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/api/v1/accounts/{id}")
    AccountDto findById(@PathVariable("id") long id);

    @Operation(summary = "List the Sleeves of an Account")
    @ApiResponse(responseCode = "200", description = "List of sleeves")
    @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/api/v1/accounts/{id}/sleeves")
    List<AccountSleeveDto> findSleeves(@PathVariable("id") long id);

    @Operation(summary = "Replace an Account in place (sleeves replaced wholesale)")
    @ApiResponse(responseCode = "200", description = "Account replaced")
    @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping(value = "/api/v1/accounts/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    AccountDto replace(@PathVariable("id") long id, @Valid @RequestBody AccountDto account);

    @Operation(summary = "Delete an Account by id (cascades to sleeves)")
    @ApiResponse(responseCode = "204", description = "Deleted (or absent)")
    @DeleteMapping("/api/v1/accounts/{id}")
    ResponseEntity<Void> deleteById(@PathVariable("id") long id);

    static URI locationOf(long id) {
        return URI.create("/api/v1/accounts/" + id);
    }
}
