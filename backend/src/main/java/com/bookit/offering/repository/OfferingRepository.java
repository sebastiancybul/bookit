package com.bookit.offering.repository;

import com.bookit.offering.entity.Offering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferingRepository extends JpaRepository<Offering, UUID> {

    List<Offering> findByBusinessIdAndActiveTrueOrderByNameAsc(UUID businessId);

    Optional<Offering> findByIdAndBusinessIdAndActiveTrue(UUID id, UUID businessId);

    Optional<Offering> findByIdAndActiveTrue(UUID id);

    long countByIdInAndBusinessIdAndActiveTrue(Collection<UUID> ids, UUID businessId);
}
