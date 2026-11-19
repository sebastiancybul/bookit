package com.bookit.business.repository;

import com.bookit.business.entity.Business;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    @EntityGraph(attributePaths = "category")
    Optional<Business> findByOwnerId(UUID ownerId);

    boolean existsByOwnerId(UUID ownerId);

    @EntityGraph(attributePaths = "category")
    Optional<Business> findByPublicIdAndActiveTrue(String publicId);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select b from Business b
            where b.active = true
              and (:cityKey is null or b.cityKey = :cityKey)
              and (:categoryCode is null or b.category.code = :categoryCode)
            """)
    Page<Business> searchActive(String cityKey, String categoryCode, Pageable pageable);
}
