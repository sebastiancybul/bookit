package com.bookit.business.controller;

import com.bookit.auth.security.AuthCookies;
import com.bookit.auth.security.CurrentUserId;
import com.bookit.business.dto.BusinessRequest;
import com.bookit.business.dto.BusinessResponse;
import com.bookit.business.service.BusinessService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Owner business")
@RestController
@RequestMapping("/owner/business")
@RequiredArgsConstructor
public class OwnerBusinessController {

    private final BusinessService businessService;
    private final AuthCookies authCookies;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(version = "1")
    public ResponseEntity<BusinessResponse> createBusiness(
            @CurrentUserId UUID userId, @Valid @RequestBody BusinessRequest request) {
        BusinessService.CreateResult result = businessService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, authCookies.accessToken(result.accessToken()).toString())
                .body(result.business());
    }

    @GetMapping(version = "1")
    public BusinessResponse getMyBusiness(@CurrentUserId UUID userId) {
        return businessService.getMine(userId);
    }

    @PutMapping(version = "1")
    public BusinessResponse updateMyBusiness(@CurrentUserId UUID userId, @Valid @RequestBody BusinessRequest request) {
        return businessService.updateMine(userId, request);
    }
}
