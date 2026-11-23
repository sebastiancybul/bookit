package com.bookit.offering.controller;

import com.bookit.auth.security.CurrentUserId;
import com.bookit.offering.dto.OfferingRequest;
import com.bookit.offering.dto.OfferingResponse;
import com.bookit.offering.service.OfferingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Owner offerings")
@RestController
@RequestMapping("/owner/business/offerings")
@RequiredArgsConstructor
public class OwnerOfferingController {

    private final OfferingService offeringService;

    @GetMapping(version = "1")
    public List<OfferingResponse> getMyOfferings(@CurrentUserId UUID userId) {
        return offeringService.getMine(userId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(version = "1")
    public OfferingResponse createOffering(@CurrentUserId UUID userId, @Valid @RequestBody OfferingRequest request) {
        return offeringService.create(userId, request);
    }

    @PutMapping(path = "/{id}", version = "1")
    public OfferingResponse updateOffering(
            @CurrentUserId UUID userId, @PathVariable UUID id, @Valid @RequestBody OfferingRequest request) {
        return offeringService.update(userId, id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "/{id}", version = "1")
    public void deleteOffering(@CurrentUserId UUID userId, @PathVariable UUID id) {
        offeringService.delete(userId, id);
    }
}
