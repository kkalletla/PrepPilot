package com.preppilot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-unit-test";

    @Test
    void roundTripsUserIdAndEmail() {
        JwtService svc = new JwtService(new JwtProperties(SECRET, 60));
        String token = svc.issue(42L, "kay@example.com");

        assertThat(svc.parse(token)).contains(new AuthenticatedUser(42L, "kay@example.com"));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService issuer = new JwtService(new JwtProperties(SECRET, 60));
        JwtService verifier = new JwtService(new JwtProperties("another-secret-another-secret-another-secret", 60));

        assertThat(verifier.parse(issuer.issue(1L, "a@b.c"))).isEmpty();
    }

    @Test
    void rejectsExpiredAndGarbageTokens() {
        JwtService svc = new JwtService(new JwtProperties(SECRET, -1));
        assertThat(svc.parse(svc.issue(1L, "a@b.c"))).isEmpty();
        assertThat(svc.parse("not.a.jwt")).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("short", 60)))
                .isInstanceOf(IllegalStateException.class);
    }
}
