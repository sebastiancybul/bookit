package com.bookit.offering.dto;

import com.bookit.offering.entity.Offering;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record OfferingResponse(
        UUID id,
        String name,
        @Schema(types = {"string", "null"}) String description,
        long priceAmount,
        String currency,
        int durationMinutes,
        int bufferMinutes) {

    public static OfferingResponse from(Offering offering) {
        return new OfferingResponse(
                offering.getId(),
                offering.getName(),
                offering.getDescription(),
                offering.getPriceAmount(),
                offering.getCurrency(),
                offering.getDurationMinutes(),
                offering.getBufferMinutes());
    }
}
