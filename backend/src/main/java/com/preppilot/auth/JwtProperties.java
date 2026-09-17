package com.preppilot.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "preppilot.jwt")
public record JwtProperties(String secret, long expiryMinutes) {}
