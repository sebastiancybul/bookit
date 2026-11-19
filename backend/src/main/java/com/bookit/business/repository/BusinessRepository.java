package com.bookit.business.repository;

import com.bookit.business.entity.Business;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    @EntityGraph(attributePaths = "category")
    Optional<Business> findByOwnerId(UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);
}
