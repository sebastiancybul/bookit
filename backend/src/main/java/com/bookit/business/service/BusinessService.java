package com.bookit.business.service;

import com.bookit.business.dto.BusinessCategoryResponse;
import com.bookit.business.dto.BusinessRequest;
import com.bookit.business.dto.BusinessResponse;
import com.bookit.business.dto.BusinessSummaryResponse;
import com.bookit.shared.web.PageResponse;
import java.util.List;
import java.util.UUID;

public interface BusinessService {

    CreateResult create(UUID ownerId, BusinessRequest request);

    BusinessResponse getMine(UUID ownerId);

    UUID getMyBusinessId(UUID ownerId);

    BusinessResponse updateMine(UUID ownerId, BusinessRequest request);

    List<BusinessCategoryResponse> getCategories();

    PageResponse<BusinessSummaryResponse> searchPublic(String city, String categoryCode, int page, int size);

    BusinessResponse getPublic(String slug);

    UUID getPublicBusinessId(String slug);

    record CreateResult(BusinessResponse business, String accessToken) {
    }
}
