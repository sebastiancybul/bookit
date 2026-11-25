package com.bookit.appointment.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bookit.booking")
public record BookingProperties(
        @NotNull @DurationMin(minutes = 0) Duration minLeadTime,
        @Positive int maxDaysAhead) {
}
