package com.bookit.business.entity;

import com.bookit.shared.BaseEntity;
import com.bookit.shared.text.Slugs;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "businesses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business extends BaseEntity {

    private static final int SLUG_MAX_LENGTH = 120;

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "owner_id", nullable = false, unique = true, updatable = false)
    private UUID ownerId;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 8)
    private String publicId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = SLUG_MAX_LENGTH)
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

    @Column(name = "city_key", nullable = false, length = 80)
    private String cityKey;

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
            String publicId,
            String name,
            String description,
            BusinessCategory category,
            String phone,
            String email,
            String street,
            String city,
            String postalCode) {
        this.ownerId = ownerId;
        this.publicId = publicId;
        applyDetails(name, description, category, phone, email, street, city, postalCode);
    }

    public void update(
            String name,
            String description,
            BusinessCategory category,
            String phone,
            String email,
            String street,
            String city,
            String postalCode) {
        applyDetails(name, description, category, phone, email, street, city, postalCode);
    }

    private void applyDetails(
            String name,
            String description,
            BusinessCategory category,
            String phone,
            String email,
            String street,
            String city,
            String postalCode) {
        String strippedEmail = stripToNull(email);
        this.name = strip(name);
        this.description = stripToNull(description);
        this.category = category;
        this.phone = strip(phone);
        this.email = strippedEmail == null ? null : strippedEmail.toLowerCase(Locale.ROOT);
        this.street = strip(street);
        this.city = strip(city);
        this.cityKey = Slugs.toSlug(this.city);
        this.postalCode = strip(postalCode);
        this.slug = buildSlug();
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String buildSlug() {
        String slug = Stream.of(publicId, Slugs.toSlug(name), Slugs.toSlug(city))
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining("-"));
        if (slug.length() <= SLUG_MAX_LENGTH) {
            return slug;
        }
        String cut = slug.substring(0, SLUG_MAX_LENGTH + 1);
        return cut.substring(0, cut.lastIndexOf('-'));
    }
}
