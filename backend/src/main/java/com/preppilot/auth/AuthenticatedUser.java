package com.preppilot.auth;

/** Principal placed in the SecurityContext by {@link JwtAuthFilter}. */
public record AuthenticatedUser(Long id, String email) {}
