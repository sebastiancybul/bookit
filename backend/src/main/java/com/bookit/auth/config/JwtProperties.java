package com.bookit.auth.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bookit.jwt")
public record JwtProperties(
        @NotNull @Size(min = 32) String secret,
        @NotNull Duration expiration,
        boolean cookieSecure) {
}
