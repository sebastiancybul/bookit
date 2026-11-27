package com.bookit.employee.dto;

import com.bookit.employee.entity.Employee;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PublicEmployeeResponse(
        UUID id,
        String displayName,
        @Schema(types = {"string", "null"}) String position,
        List<UUID> offeringIds) {

    public static PublicEmployeeResponse from(Employee employee, Set<UUID> activeOfferingIds) {
        return new PublicEmployeeResponse(
                employee.getId(),
                employee.getDisplayName(),
                employee.getPosition(),
                employee.getOfferingIds().stream().filter(activeOfferingIds::contains).sorted().toList());
    }
}
