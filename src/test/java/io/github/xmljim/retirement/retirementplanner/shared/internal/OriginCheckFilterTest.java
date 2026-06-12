/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.github.xmljim.retirement.retirementplanner.shared.CorsProperties;

class OriginCheckFilterTest {

    private static final String ALLOWED = "https://app.example.com";
    private static final String BLOCKED = "https://evil.example.com";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String PATH = "/api/v1/plans";
    private static final int STATUS_OK = 200;
    private static final int STATUS_FORBIDDEN = 403;

    private final CorsProperties cors = new CorsProperties(
            List.of(ALLOWED), List.of(METHOD_GET, METHOD_POST), List.of("*"), false, Duration.ofHours(1));

    private final OriginCheckFilter filter = new OriginCheckFilter(cors);

    @Test
    @DisplayName("GET passes through regardless of origin")
    void getPassesThroughIgnoringOrigin() throws Exception {
        var request = new MockHttpServletRequest(METHOD_GET, PATH);
        request.addHeader(HttpHeaders.ORIGIN, BLOCKED);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(STATUS_OK);
    }

    @Test
    @DisplayName("POST without Origin or Referer passes (non-browser client)")
    void postWithoutOriginPasses() throws Exception {
        var request = new MockHttpServletRequest(METHOD_POST, PATH);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(STATUS_OK);
    }

    @Test
    @DisplayName("POST with allowed Origin passes")
    void postWithAllowedOriginPasses() throws Exception {
        var request = new MockHttpServletRequest(METHOD_POST, PATH);
        request.addHeader(HttpHeaders.ORIGIN, ALLOWED);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(STATUS_OK);
    }

    @Test
    @DisplayName("POST with disallowed Origin returns 403")
    void postWithDisallowedOriginIsForbidden() throws Exception {
        var request = new MockHttpServletRequest(METHOD_POST, PATH);
        request.addHeader(HttpHeaders.ORIGIN, BLOCKED);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(STATUS_FORBIDDEN);
    }

    @Test
    @DisplayName("Referer URL with allowed origin passes")
    void postWithAllowedRefererPasses() throws Exception {
        var request = new MockHttpServletRequest("PUT", PATH);
        request.addHeader(HttpHeaders.REFERER, ALLOWED + "/some/path");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(STATUS_OK);
    }

    @Test
    @DisplayName("Referer URL from disallowed origin returns 403")
    void postWithDisallowedRefererIsForbidden() throws Exception {
        var request = new MockHttpServletRequest("DELETE", PATH);
        request.addHeader(HttpHeaders.REFERER, BLOCKED + "/some/path");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(STATUS_FORBIDDEN);
    }
}
