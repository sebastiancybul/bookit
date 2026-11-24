package com.bookit.employee.repository;

import com.bookit.employee.entity.Employee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    @EntityGraph(attributePaths = "offeringIds")
    List<Employee> findByBusinessIdAndActiveTrueOrderByDisplayNameAsc(UUID businessId);

    @EntityGraph(attributePaths = "offeringIds")
    Optional<Employee> findByIdAndBusinessIdAndActiveTrue(UUID id, UUID businessId);

    boolean existsByUserIdAndIdNot(UUID userId, UUID id);

    boolean existsByIdAndBusinessIdAndActiveTrue(UUID id, UUID businessId);
}
