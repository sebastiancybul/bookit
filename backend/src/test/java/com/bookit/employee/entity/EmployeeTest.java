package com.bookit.employee.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmployeeTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();

    @Test
    void stripsDisplayNameAndTurnsBlankPositionIntoNull() {
        Employee employee = new Employee(BUSINESS_ID, "  Anna K.  ", "   ");

        assertThat(employee.getDisplayName()).isEqualTo("Anna K.");
        assertThat(employee.getPosition()).isNull();
        assertThat(employee.isActive()).isTrue();
    }

    @Test
    void updateStripsPosition() {
        Employee employee = new Employee(BUSINESS_ID, "Anna K.", null);

        employee.update("Anna K.", "  Senior barber ");

        assertThat(employee.getPosition()).isEqualTo("Senior barber");
    }

    @Test
    void replaceOfferingsReplacesWholeSet() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Employee employee = new Employee(BUSINESS_ID, "Anna K.", null);
        employee.replaceOfferings(List.of(first));

        employee.replaceOfferings(List.of(second));

        assertThat(employee.getOfferingIds()).containsExactly(second);
    }

    @Test
    void deactivateUnlinksAccount() {
        Employee employee = new Employee(BUSINESS_ID, "Anna K.", null);
        employee.linkAccount(UUID.randomUUID());

        employee.deactivate();

        assertThat(employee.isActive()).isFalse();
        assertThat(employee.getUserId()).isNull();
    }
}
