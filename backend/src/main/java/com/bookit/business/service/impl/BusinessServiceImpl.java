package com.bookit.business.service.impl;

import com.bookit.auth.security.JwtService;
import com.bookit.business.dto.BusinessRequest;
import com.bookit.business.dto.BusinessResponse;
import com.bookit.business.entity.Business;
import com.bookit.business.entity.BusinessCategory;
import com.bookit.business.repository.BusinessCategoryRepository;
import com.bookit.business.repository.BusinessRepository;
import com.bookit.business.service.BusinessService;
import com.bookit.shared.error.BusinessRuleException;
import com.bookit.shared.error.InvalidFieldException;
import com.bookit.shared.error.ResourceNotFoundException;
import com.bookit.user.entity.Role;
import com.bookit.user.entity.User;
import com.bookit.user.repository.RoleRepository;
import com.bookit.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private static final String PUBLIC_ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PUBLIC_ID_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BusinessRepository businessRepository;
    private final BusinessCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public CreateResult create(UUID ownerId, BusinessRequest request) {
        if (businessRepository.existsByOwnerId(ownerId)) {
            throw businessAlreadyExists();
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
        Role ownerRole = roleRepository.findByName("ROLE_OWNER")
                .orElseThrow(() -> new IllegalStateException("Role ROLE_OWNER is missing"));

        Business business = new Business(
                ownerId,
                newPublicId(),
                request.name(),
                request.description(),
                findCategory(request.categoryCode()),
                request.phone(),
                request.email(),
                request.street(),
                request.city(),
                request.postalCode());
        try {
            businessRepository.saveAndFlush(business);
        } catch (DataIntegrityViolationException ex) {
            if (violates(ex, "uk_businesses_owner")) {
                throw businessAlreadyExists();
            }
            throw ex;
        }
        owner.addRole(ownerRole);
        return new CreateResult(BusinessResponse.from(business), jwtService.issueToken(owner));
    }

    @Override
    public BusinessResponse getMine(UUID ownerId) {
        return BusinessResponse.from(findMine(ownerId));
    }

    @Override
    @Transactional
    public BusinessResponse updateMine(UUID ownerId, BusinessRequest request) {
        Business business = findMine(ownerId);
        business.update(
                request.name(),
                request.description(),
                findCategory(request.categoryCode()),
                request.phone(),
                request.email(),
                request.street(),
                request.city(),
                request.postalCode());
        return BusinessResponse.from(business);
    }

    private Business findMine(UUID ownerId) {
        return businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Business of owner", ownerId));
    }

    private BusinessCategory findCategory(String code) {
        return categoryRepository.findByCode(code)
                .orElseThrow(() -> new InvalidFieldException("categoryCode", "Unknown category"));
    }

    private static String newPublicId() {
        StringBuilder builder = new StringBuilder(PUBLIC_ID_LENGTH);
        for (int i = 0; i < PUBLIC_ID_LENGTH; i++) {
            builder.append(PUBLIC_ID_ALPHABET.charAt(RANDOM.nextInt(PUBLIC_ID_ALPHABET.length())));
        }
        return builder.toString();
    }

    private static boolean violates(DataIntegrityViolationException ex, String constraintName) {
        return ex.getCause() instanceof ConstraintViolationException cause
                && constraintName.equals(cause.getConstraintName());
    }

    private static BusinessRuleException businessAlreadyExists() {
        return new BusinessRuleException("BUSINESS_ALREADY_EXISTS", "You already have a business");
    }
}
