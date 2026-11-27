package com.bookit.employee.repository;

import com.bookit.employee.entity.Employee;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    @EntityGraph(attributePaths = "offeringIds")
    List<Employee> findByBusinessIdAndActiveTrueOrderByDisplayNameAsc(UUID businessId);

    @EntityGraph(attributePaths = "offeringIds")
    Optional<Employee> findByIdAndBusinessIdAndActiveTrue(UUID id, UUID businessId);

    boolean existsByUserIdAndIdNot(UUID userId, UUID id);

    boolean existsByIdAndBusinessIdAndActiveTrue(UUID id, UUID businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id and e.businessId = :businessId and e.active = true")
    Optional<Employee> findActiveForUpdate(@Param("id") UUID id, @Param("businessId") UUID businessId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from Employee e where e.id = :id and e.active = true")
    Optional<Employee> findActiveForShare(@Param("id") UUID id);
}
