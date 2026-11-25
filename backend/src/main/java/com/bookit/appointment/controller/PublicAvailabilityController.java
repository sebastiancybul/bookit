package com.bookit.appointment.controller;

import com.bookit.appointment.dto.AvailableSlotResponse;
import com.bookit.appointment.service.AvailabilityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Businesses")
@RestController
@RequestMapping("/businesses/{slug}/availability")
@RequiredArgsConstructor
public class PublicAvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping(version = "1")
    public List<AvailableSlotResponse> getAvailableSlots(
            @PathVariable String slug,
            @RequestParam UUID offeringId,
            @RequestParam UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availabilityService.getAvailableSlots(slug, offeringId, employeeId, date);
    }
}
