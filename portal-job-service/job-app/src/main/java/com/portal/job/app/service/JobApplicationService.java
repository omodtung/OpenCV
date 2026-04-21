package com.portal.job.app.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.job.app.messaging.JobMessageSender;
import com.portal.job.domain.Job;
import com.portal.job.domain.JobApplication;
import com.portal.job.domain.JobApplicationRepository;
import com.portal.job.domain.JobRepository;

@Service
public class JobApplicationService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobMessageSender jobMessageSender;

    @Transactional
    public JobApplication createApplicationFromEvent(UUID jobId, UUID candidateId, String candidateName, UUID resumeId) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setCandidateId(candidateId);
        application.setCandidateName(candidateName);
        application.setResumeId(resumeId);
        return jobApplicationRepository.save(application);
    }

    @Transactional
    public JobApplication updateApplicationStatus(UUID applicationId, String status) {
        JobApplication application = jobApplicationRepository.findById(applicationId).orElseThrow();
        application.setStatus(status);
        application = jobApplicationRepository.save(application);

        Map<String, String> headers = jobMessageSender.getDefaultMessageHeaders(applicationId.toString());
        jobMessageSender.sendApplicationStatusChanged(
                "{\"applicationId\":\"" + applicationId + "\",\"status\":\"" + status + "\"}",
                headers
        );
        return application;
    }
}