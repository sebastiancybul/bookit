package com.bookit.offering.service.impl;

import com.bookit.business.service.BusinessService;
import com.bookit.offering.dto.OfferingRequest;
import com.bookit.offering.dto.OfferingResponse;
import com.bookit.offering.dto.PublicOfferingResponse;
import com.bookit.offering.entity.Offering;
import com.bookit.offering.repository.OfferingRepository;
import com.bookit.offering.service.OfferingService;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferingServiceImpl implements OfferingService {

    private static final String NAME_CONSTRAINT = "uk_offerings_business_name_active";

    private final OfferingRepository offeringRepository;
    private final BusinessService businessService;

    @Override
    public List<OfferingResponse> getMine(UUID ownerId) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        return offeringRepository.findByBusinessIdAndActiveTrueOrderByNameAsc(businessId).stream()
                .map(OfferingResponse::from)
                .toList();
    }

    @Override
    public OfferingResponse create(UUID ownerId, OfferingRequest request) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        Offering offering = new Offering(
                businessId,
                request.name(),
                request.description(),
                request.priceAmount(),
                request.durationMinutes(),
                request.bufferMinutes());
        try {
            offeringRepository.saveAndFlush(offering);
        } catch (DataIntegrityViolationException ex) {
            throw translateNameConflict(ex);
        }
        return OfferingResponse.from(offering);
    }

    @Override
    @Transactional
    public OfferingResponse update(UUID ownerId, UUID offeringId, OfferingRequest request) {
        Offering offering = findMine(ownerId, offeringId);
        offering.update(
                request.name(),
                request.description(),
                request.priceAmount(),
                request.durationMinutes(),
                request.bufferMinutes());
        try {
            offeringRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translateNameConflict(ex);
        }
        return OfferingResponse.from(offering);
    }

    @Override
    @Transactional
    public void delete(UUID ownerId, UUID offeringId) {
        findMine(ownerId, offeringId).deactivate();
    }

    @Override
    public List<PublicOfferingResponse> getPublic(String businessSlug) {
        UUID businessId = businessService.getPublicBusinessId(businessSlug);
        return offeringRepository.findByBusinessIdAndActiveTrueOrderByNameAsc(businessId).stream()
                .map(PublicOfferingResponse::from)
                .toList();
    }

    private Offering findMine(UUID ownerId, UUID offeringId) {
        UUID businessId = businessService.getMyBusinessId(ownerId);
        return offeringRepository.findByIdAndBusinessIdAndActiveTrue(offeringId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering", offeringId));
    }

    private static RuntimeException translateNameConflict(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException cause
                && NAME_CONSTRAINT.equals(cause.getConstraintName())) {
            return new BusinessRuleException("OFFERING_NAME_TAKEN", "An offering with this name already exists");
        }
        return ex;
    }
}
