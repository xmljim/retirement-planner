/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import io.github.xmljim.retirement.retirementplanner.shared.AuthProperties;
import io.github.xmljim.retirement.retirementplanner.shared.CorsProperties;

/**
 * Wires the {@link SecurityFilterChain} for the active auth mode.
 *
 * <p><strong>Stub mode</strong> ({@link AuthProperties.Mode#STUB}) installs a
 * fixed {@link io.github.xmljim.retirement.retirementplanner.shared.AuthenticatedUser}
 * for the seeded {@code solo} tenant on every request. CSRF is disabled
 * because there is no authenticated session to protect — the client is
 * a fixed identity, not one established by login. Sessions are
 * stateless. CORS uses the {@link CorsProperties} allowlist; the
 * {@link OriginCheckFilter} layers on a defense-in-depth check on
 * state-changing methods.
 *
 * <p><strong>Passkey mode</strong> ({@link AuthProperties.Mode#PASSKEY}) is a
 * placeholder until EPIC-8 wires WebAuthn end-to-end. Selecting it
 * today fails fast at startup with a pointer to the issue.
 *
 * <p><strong>SaaS transport (TBD).</strong> When EPIC-8 lands, the bearer-vs-
 * cookie decision will determine whether Spring Security's CSRF filter
 * needs to come back. If a session cookie is used, re-enable CSRF with
 * {@code CookieCsrfTokenRepository}; if the API stays bearer-only, CSRF
 * remains disabled and the {@link OriginCheckFilter} carries the same
 * defense-in-depth role.
 */
// HttpSecurity.build() declares Exception in the Spring Security API; bubbling it is the
// canonical pattern for a SecurityFilterChain @Bean.
@SuppressWarnings("PMD.SignatureDeclareThrowsException")
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({AuthProperties.class, CorsProperties.class})
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthProperties auth,
            CorsProperties cors,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource)
            throws Exception {
        return switch (auth.mode()) {
            case STUB -> stubChain(http, auth, cors, corsSource);
            case PASSKEY ->
                throw new IllegalStateException("Passkey auth mode is not yet implemented "
                        + "(tracked in EPIC-8). Set app.auth.mode=stub for local development.");
        };
    }

    private SecurityFilterChain stubChain(
            HttpSecurity http, AuthProperties auth, CorsProperties cors, CorsConfigurationSource corsSource)
            throws Exception {
        var stub = new StubAuthenticationFilter(auth.soloPrincipal());
        var origin = new OriginCheckFilter(cors);
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg.anyRequest().authenticated())
                .addFilterBefore(stub, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(origin, StubAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(props.allowedOrigins());
        config.setAllowedMethods(props.allowedMethods());
        config.setAllowedHeaders(props.allowedHeaders());
        config.setAllowCredentials(props.allowCredentials());
        config.setMaxAge(props.maxAge());
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
