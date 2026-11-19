package com.bookit.business.repository;

import com.bookit.business.entity.BusinessCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, Long> {

    Optional<BusinessCategory> findByCode(String code);
}
