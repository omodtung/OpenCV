package com.portal.employer.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployerRepository extends JpaRepository<Employer, UUID> {
    Optional<Employer> findByUserId(UUID userId);
}