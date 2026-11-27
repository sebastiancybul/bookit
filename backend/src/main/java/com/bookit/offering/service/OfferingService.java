package com.bookit.offering.service;

import com.bookit.offering.dto.OfferingRequest;
import com.bookit.offering.dto.OfferingResponse;
import com.bookit.offering.dto.PublicOfferingResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface OfferingService {

    List<OfferingResponse> getMine(UUID ownerId);

    OfferingResponse create(UUID ownerId, OfferingRequest request);

    OfferingResponse update(UUID ownerId, UUID offeringId, OfferingRequest request);

    void delete(UUID ownerId, UUID offeringId);

    List<PublicOfferingResponse> getPublic(String businessSlug);

    boolean allActiveInBusiness(Set<UUID> offeringIds, UUID businessId);

    Set<UUID> activeIdsInBusiness(UUID businessId);

    BookableOffering getBookable(UUID offeringId);

    record BookableOffering(
            UUID id,
            UUID businessId,
            String name,
            int durationMinutes,
            int bufferMinutes,
            long priceAmount,
            String currency) {
    }
}
