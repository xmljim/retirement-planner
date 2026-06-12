/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Authentication configuration bound from {@code app.auth.*}.
 *
 * <p>{@link Mode#STUB} installs a fixed solo principal and bypasses
 * login — used for local development and integration tests. The
 * principal's username is configurable so tests can assert on it
 * without coupling to a hard-coded constant.
 *
 * <p>{@link Mode#PASSKEY} is a placeholder; real WebAuthn wiring lands
 * in EPIC-8. Selecting {@code passkey} today fails fast at startup.
 *
 * <p>The {@code prod} profile omits a default and must set the mode
 * explicitly; the {@code local} profile defaults to {@code stub}.
 */
@ConfigurationProperties("app.auth")
@Validated
public record AuthProperties(@NotNull Mode mode, @NotBlank String soloPrincipal) {

    /** Auth backend selector. */
    public enum Mode {
        /** Fixed solo principal; no real authentication. Local + tests. */
        STUB,
        /** WebAuthn / passkey; placeholder until EPIC-8 wires it. */
        PASSKEY
    }
}
