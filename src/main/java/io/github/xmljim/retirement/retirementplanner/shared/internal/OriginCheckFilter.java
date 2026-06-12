/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.xmljim.retirement.retirementplanner.shared.CorsProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Defense-in-depth check on state-changing methods: when the browser
 * sends an {@code Origin} (or {@code Referer}) header, it must match
 * the CORS allowlist. Non-browser clients (curl, server-to-server)
 * typically omit both and pass through — bearer-token authorization
 * still gates access for those callers.
 *
 * <p>This is NOT Spring Security's CSRF filter, which guards
 * cookie/session flows; it's a complementary check for bearer-token
 * APIs against browser-mediated replay (a leaked token used from an
 * attacker-controlled origin).
 *
 * <p>If the allowlist is empty (defaults / unconfigured prod), the
 * filter rejects all browser requests on state-changing methods —
 * fail-closed.
 */
final class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> STATE_CHANGING =
            Set.of(HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name());

    private final Set<String> allowedOrigins;

    OriginCheckFilter(CorsProperties cors) {
        super();
        this.allowedOrigins = Set.copyOf(cors.allowedOrigins());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!STATE_CHANGING.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String referer = origin == null ? request.getHeader(HttpHeaders.REFERER) : null;
        String candidate = origin != null ? origin : referer;
        if (candidate == null) {
            // No browser origin metadata — non-browser client. Bearer/auth gates this path.
            chain.doFilter(request, response);
            return;
        }
        if (!isAllowed(candidate)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin not allowed");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAllowed(String candidate) {
        if (allowedOrigins.contains(candidate)) {
            return true;
        }
        // Referer carries a full URL; compare its origin (scheme://host[:port]).
        try {
            URI uri = new URI(candidate);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return false;
            }
            int port = uri.getPort();
            String origin = port < 0
                    ? uri.getScheme() + "://" + uri.getHost()
                    : uri.getScheme() + "://" + uri.getHost() + ":" + port;
            return allowedOrigins.contains(origin);
        } catch (URISyntaxException ex) {
            return false;
        }
    }
}
