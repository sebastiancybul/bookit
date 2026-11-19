package com.bookit.business.service;

import com.bookit.business.dto.BusinessRequest;
import com.bookit.business.dto.BusinessResponse;
import java.util.UUID;

public interface BusinessService {

    CreateResult create(UUID ownerId, BusinessRequest request);

    BusinessResponse getMine(UUID ownerId);

    BusinessResponse updateMine(UUID ownerId, BusinessRequest request);

    record CreateResult(BusinessResponse business, String accessToken) {
    }
}
