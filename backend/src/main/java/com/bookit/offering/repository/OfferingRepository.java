package com.bookit.offering.repository;

import com.bookit.offering.entity.Offering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferingRepository extends JpaRepository<Offering, UUID> {

    List<Offering> findByBusinessIdAndActiveTrueOrderByNameAsc(UUID businessId);

    Optional<Offering> findByIdAndBusinessIdAndActiveTrue(UUID id, UUID businessId);

    Optional<Offering> findByIdAndActiveTrue(UUID id);

    long countByIdInAndBusinessIdAndActiveTrue(Collection<UUID> ids, UUID businessId);

    @Query("select o.id from Offering o where o.businessId = :businessId and o.active = true")
    Set<UUID> findActiveIdsByBusinessId(@Param("businessId") UUID businessId);
}
