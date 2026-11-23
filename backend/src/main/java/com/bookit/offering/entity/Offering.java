package com.bookit.offering.entity;

import com.bookit.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "offerings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offering extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    private UUID businessId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price_amount", nullable = false)
    private long priceAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PLN";

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Offering(
            UUID businessId,
            String name,
            String description,
            long priceAmount,
            int durationMinutes,
            int bufferMinutes) {
        this.businessId = businessId;
        applyDetails(name, description, priceAmount, durationMinutes, bufferMinutes);
    }

    public void update(
            String name,
            String description,
            long priceAmount,
            int durationMinutes,
            int bufferMinutes) {
        applyDetails(name, description, priceAmount, durationMinutes, bufferMinutes);
    }

    public void deactivate() {
        this.active = false;
    }

    private void applyDetails(
            String name,
            String description,
            long priceAmount,
            int durationMinutes,
            int bufferMinutes) {
        this.name = name == null ? null : name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.priceAmount = priceAmount;
        this.durationMinutes = durationMinutes;
        this.bufferMinutes = bufferMinutes;
    }
}
