package com.bookit.business.repository;

import com.bookit.business.entity.BusinessCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, Long> {}
