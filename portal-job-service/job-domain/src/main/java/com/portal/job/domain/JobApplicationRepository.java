package com.portal.job.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    List<JobApplication> findByJobId(UUID jobId);

    List<JobApplication> findByCandidateId(UUID candidateId);
}