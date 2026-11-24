package com.bookit.schedule.repository;

import com.bookit.schedule.entity.WorkSchedule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {

    @EntityGraph(attributePaths = "intervals")
    Optional<WorkSchedule> findByEmployeeId(UUID employeeId);
}
