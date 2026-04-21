package com.portal.candidate.app.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.candidate.app.messaging.CandidateMessageSender;
import com.portal.candidate.domain.AppliedJob;
import com.portal.candidate.domain.AppliedJobRepository;
import com.portal.candidate.domain.Candidate;
import com.portal.candidate.domain.CandidateRepository;

@Service
public class CandidateProfileService {

    private final CandidateRepository candidateRepository;
    private final AppliedJobRepository appliedJobRepository;
    private final CandidateMessageSender messageSender;

    public CandidateProfileService(CandidateRepository candidateRepository,
                                   AppliedJobRepository appliedJobRepository,
                                   CandidateMessageSender messageSender) {
        this.candidateRepository = candidateRepository;
        this.appliedJobRepository = appliedJobRepository;
        this.messageSender = messageSender;
    }

    @Transactional
    public Candidate createProfile(UUID userId, String email) {
        Candidate candidate = new Candidate();
        candidate.setUserId(userId);
        candidate.setEmail(email);
        return candidateRepository.save(candidate);
    }

    @Transactional
    public AppliedJob applyJob(UUID candidateId, UUID jobId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        AppliedJob appliedJob = new AppliedJob();
        appliedJob.setCandidate(candidate);
        appliedJob.setJobId(jobId);
        AppliedJob saved = appliedJobRepository.save(appliedJob);

        Map<String, String> headers = messageSender.getDefaultMessageHeaders(candidateId.toString());
        headers.put("candidateId", candidateId.toString());
        headers.put("jobId", jobId.toString());
        messageSender.sendJobApplied("", headers);

        return saved;
    }
}