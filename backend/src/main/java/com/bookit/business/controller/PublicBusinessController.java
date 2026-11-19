package com.bookit.business.controller;

import com.bookit.business.dto.BusinessResponse;
import com.bookit.business.dto.BusinessSummaryResponse;
import com.bookit.business.service.BusinessService;
import com.bookit.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Businesses")
@RestController
@RequestMapping("/businesses")
@RequiredArgsConstructor
public class PublicBusinessController {

    private final BusinessService businessService;

    @GetMapping(version = "1")
    public PageResponse<BusinessSummaryResponse> searchBusinesses(
            @RequestParam(required = false) @Size(max = 80) String city,
            @RequestParam(required = false) @Size(max = 30) String category,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return businessService.searchPublic(city, category, page, size);
    }

    @GetMapping(path = "/{slug}", version = "1")
    public BusinessResponse getBusinessBySlug(@PathVariable String slug) {
        return businessService.getPublic(slug);
    }
}
