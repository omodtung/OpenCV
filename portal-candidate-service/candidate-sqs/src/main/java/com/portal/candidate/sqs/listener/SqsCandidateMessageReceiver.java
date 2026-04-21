package com.portal.candidate.sqs.listener;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.portal.candidate.app.service.CandidateProfileService;
import com.portal.candidate.domain.AppliedJob;
import com.portal.candidate.domain.AppliedJobRepository;

import io.awspring.cloud.sqs.annotation.SqsListener;

@Component
@Profile("sqs")
public class SqsCandidateMessageReceiver {

    private final CandidateProfileService candidateProfileService;
    private final AppliedJobRepository appliedJobRepository;

    public SqsCandidateMessageReceiver(CandidateProfileService candidateProfileService,
                                       AppliedJobRepository appliedJobRepository) {
        this.candidateProfileService = candidateProfileService;
        this.appliedJobRepository = appliedJobRepository;
    }

    @SqsListener("${queue.user.registered}")
    public void receiveUserRegistered(@Header("role") String role,
                                      @Header("userId") String userId,
                                      @Header("email") String email) {
        if ("ROLE_CANDIDATE".equals(role)) {
            candidateProfileService.createProfile(UUID.fromString(userId), email);
        }
    }

    @SqsListener("${queue.application.status-changed}")
    @Transactional
    public void receiveApplicationStatusChanged(@Header("jobId") String jobId,
                                                @Header("status") String status) {
        List<AppliedJob> jobs = appliedJobRepository.findByJobId(UUID.fromString(jobId));
        jobs.forEach(j -> j.setApplicationStatus(status));
        appliedJobRepository.saveAll(jobs);
    }
}