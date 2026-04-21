package com.portal.candidate.jms.listener;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.portal.candidate.app.service.CandidateProfileService;
import com.portal.candidate.domain.AppliedJob;
import com.portal.candidate.domain.AppliedJobRepository;

import jakarta.jms.Message;

@Component
@Profile("jms")
public class JmsCandidateMessageReceiver {

    private final CandidateProfileService candidateProfileService;
    private final AppliedJobRepository appliedJobRepository;

    public JmsCandidateMessageReceiver(CandidateProfileService candidateProfileService,
                                       AppliedJobRepository appliedJobRepository) {
        this.candidateProfileService = candidateProfileService;
        this.appliedJobRepository = appliedJobRepository;
    }

    @JmsListener(destination = "${queue.user.registered}")
    public void receiveUserRegistered(Message message) throws Exception {
        String role = message.getStringProperty("role");
        if ("ROLE_CANDIDATE".equals(role)) {
            UUID userId = UUID.fromString(message.getStringProperty("userId"));
            String email = message.getStringProperty("email");
            candidateProfileService.createProfile(userId, email);
        }
    }

    @JmsListener(destination = "${queue.application.status-changed}")
    @Transactional
    public void receiveApplicationStatusChanged(Message message) throws Exception {
        UUID jobId = UUID.fromString(message.getStringProperty("jobId"));
        String status = message.getStringProperty("status");
        List<AppliedJob> jobs = appliedJobRepository.findByJobId(jobId);
        jobs.forEach(j -> j.setApplicationStatus(status));
        appliedJobRepository.saveAll(jobs);
    }
}