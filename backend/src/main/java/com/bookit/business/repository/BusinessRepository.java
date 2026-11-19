package com.bookit.business.repository;

import com.bookit.business.entity.Business;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, UUID> {}
