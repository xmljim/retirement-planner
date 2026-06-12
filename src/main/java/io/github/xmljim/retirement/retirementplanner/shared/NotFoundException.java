/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

/**
 * Thrown by services when an aggregate or sub-entity is not found in
 * the active tenant. The REST layer maps it to a 404 problem+json
 * response.
 *
 * <p>Generic (no aggregate-specific subclasses) so every module can
 * throw it without coupling to a sibling module's exception hierarchy.
 */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
