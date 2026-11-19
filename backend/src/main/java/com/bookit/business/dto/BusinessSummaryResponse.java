package com.bookit.business.dto;

import com.bookit.business.entity.Business;

public record BusinessSummaryResponse(
        String publicId,
        String slug,
        String name,
        BusinessCategoryResponse category,
        String city,
        String street) {

    public static BusinessSummaryResponse from(Business business) {
        return new BusinessSummaryResponse(
                business.getPublicId(),
                business.getSlug(),
                business.getName(),
                BusinessCategoryResponse.from(business.getCategory()),
                business.getCity(),
                business.getStreet());
    }
}
