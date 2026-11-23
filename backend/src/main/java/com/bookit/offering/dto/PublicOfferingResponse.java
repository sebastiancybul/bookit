package com.bookit.offering.dto;

import com.bookit.offering.entity.Offering;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record PublicOfferingResponse(
        UUID id,
        String name,
        @Schema(types = {"string", "null"}) String description,
        long priceAmount,
        String currency,
        int durationMinutes) {

    public static PublicOfferingResponse from(Offering offering) {
        return new PublicOfferingResponse(
                offering.getId(),
                offering.getName(),
                offering.getDescription(),
                offering.getPriceAmount(),
                offering.getCurrency(),
                offering.getDurationMinutes());
    }
}
