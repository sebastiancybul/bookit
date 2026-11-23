package com.bookit.offering.controller;

import com.bookit.offering.dto.PublicOfferingResponse;
import com.bookit.offering.service.OfferingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Businesses")
@RestController
@RequestMapping("/businesses/{slug}/offerings")
@RequiredArgsConstructor
public class PublicOfferingController {

    private final OfferingService offeringService;

    @GetMapping(version = "1")
    public List<PublicOfferingResponse> getBusinessOfferings(@PathVariable String slug) {
        return offeringService.getPublic(slug);
    }
}
