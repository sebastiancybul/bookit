package com.bookit.business.dto;

import com.bookit.business.entity.BusinessCategory;

public record BusinessCategoryResponse(
        String code,
        String name) {

    public static BusinessCategoryResponse from(BusinessCategory category) {
        return new BusinessCategoryResponse(category.getCode(), category.getName());
    }
}
