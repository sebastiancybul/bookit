package com.bookit.business.entity;

import com.bookit.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "businesses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "owner_id", nullable = false, unique = true, updatable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private BusinessCategory category;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "street", nullable = false, length = 150)
    private String street;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 6)
    private String postalCode;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Europe/Warsaw";

    @Column(name = "free_cancellation_hours", nullable = false)
    private int freeCancellationHours = 24;

    @Column(name = "slot_interval_minutes", nullable = false)
    private int slotIntervalMinutes = 15;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Business(
            UUID ownerId,
            String name,
            String slug,
            String description,
            BusinessCategory category,
            String phone,
            String email,
            String street,
            String city,
            String postalCode) {
        this.ownerId = ownerId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.category = category;
        this.phone = phone;
        this.email = email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
    }
}
