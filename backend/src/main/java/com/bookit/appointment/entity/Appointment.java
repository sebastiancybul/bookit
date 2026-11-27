package com.bookit.appointment.entity;

import com.bookit.offering.service.OfferingService.BookableOffering;
import com.bookit.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    private UUID businessId;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "offering_id", nullable = false, updatable = false)
    private UUID offeringId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "start_time", nullable = false, updatable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false, updatable = false)
    private Instant endTime;

    @Column(name = "blocked_until", nullable = false, updatable = false)
    private Instant blockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "offering_name", nullable = false, length = 100, updatable = false)
    private String offeringName;

    @Column(name = "price_amount", nullable = false, updatable = false)
    private long priceAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "client_note", length = 500, updatable = false)
    private String clientNote;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 20)
    private CancelledBy cancelledBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Appointment book(
            UUID clientId,
            UUID employeeId,
            BookableOffering offering,
            Instant startTime,
            String clientNote,
            UUID idempotencyKey) {
        Appointment appointment = new Appointment();
        appointment.businessId = offering.businessId();
        appointment.employeeId = employeeId;
        appointment.offeringId = offering.id();
        appointment.clientId = clientId;
        appointment.startTime = startTime;
        appointment.endTime = startTime.plus(Duration.ofMinutes(offering.durationMinutes()));
        appointment.blockedUntil = appointment.endTime.plus(Duration.ofMinutes(offering.bufferMinutes()));
        appointment.status = AppointmentStatus.CONFIRMED;
        appointment.offeringName = offering.name();
        appointment.priceAmount = offering.priceAmount();
        appointment.currency = offering.currency();
        appointment.clientNote = normalizeNote(clientNote);
        appointment.idempotencyKey = idempotencyKey;
        return appointment;
    }

    public static String normalizeNote(String clientNote) {
        return clientNote == null || clientNote.isBlank() ? null : clientNote.strip();
    }
}
