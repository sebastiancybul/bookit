package com.bookit.appointment.repository;

import com.bookit.appointment.entity.Appointment;
import com.bookit.schedule.slot.BusyRange;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByClientIdAndIdempotencyKey(UUID clientId, UUID idempotencyKey);

    @Query("""
            select new com.bookit.schedule.slot.BusyRange(a.startTime, a.blockedUntil)
            from Appointment a
            where a.employeeId = :employeeId
              and a.status = com.bookit.appointment.entity.AppointmentStatus.CONFIRMED
              and a.startTime < :to
              and a.blockedUntil > :from
            """)
    List<BusyRange> findBusyRanges(
            @Param("employeeId") UUID employeeId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
