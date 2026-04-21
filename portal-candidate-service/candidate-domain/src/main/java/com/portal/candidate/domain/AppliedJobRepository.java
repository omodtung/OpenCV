package com.portal.candidate.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedJobRepository extends JpaRepository<AppliedJob, UUID> {
    List<AppliedJob> findByCandidateId(UUID candidateId);
    List<AppliedJob> findByJobId(UUID jobId);
}