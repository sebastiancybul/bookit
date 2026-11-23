package com.bookit.employee.entity;

import com.bookit.shared.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "employees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    private UUID businessId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "position", length = 80)
    private String position;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(name = "employee_offerings", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "offering_id", nullable = false)
    private Set<UUID> offeringIds = new HashSet<>();

    public Employee(UUID businessId, String displayName, String position) {
        this.businessId = businessId;
        applyDetails(displayName, position);
    }

    public void update(String displayName, String position) {
        applyDetails(displayName, position);
    }

    public void replaceOfferings(Collection<UUID> offeringIds) {
        this.offeringIds.clear();
        this.offeringIds.addAll(offeringIds);
    }

    public void linkAccount(UUID userId) {
        this.userId = userId;
    }

    public void unlinkAccount() {
        this.userId = null;
    }

    public void deactivate() {
        unlinkAccount();
        this.active = false;
    }

    private void applyDetails(String displayName, String position) {
        this.displayName = displayName == null ? null : displayName.strip();
        this.position = position == null || position.isBlank() ? null : position.strip();
    }
}
