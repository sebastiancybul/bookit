package com.bookit.business.dto;

import com.bookit.business.entity.Business;
import io.swagger.v3.oas.annotations.media.Schema;

public record BusinessResponse(
        String publicId,
        String slug,
        String name,
        @Schema(types = {"string", "null"}) String description,
        BusinessCategoryResponse category,
        String phone,
        @Schema(types = {"string", "null"}) String email,
        String street,
        String city,
        String postalCode,
        String timezone,
        int freeCancellationHours,
        int slotIntervalMinutes) {

    public static BusinessResponse from(Business business) {
        return new BusinessResponse(
                business.getPublicId(),
                business.getSlug(),
                business.getName(),
                business.getDescription(),
                BusinessCategoryResponse.from(business.getCategory()),
                business.getPhone(),
                business.getEmail(),
                business.getStreet(),
                business.getCity(),
                business.getPostalCode(),
                business.getTimezone(),
                business.getFreeCancellationHours(),
                business.getSlotIntervalMinutes());
    }
}
