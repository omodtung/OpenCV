package org.example.candidateservice.repository;

import org.example.candidateservice.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    Optional<Candidate> findByEmail(String email);
    // You might also need to find by userId if it's external
    Optional<Candidate> findByUserId(UUID userId);
}
