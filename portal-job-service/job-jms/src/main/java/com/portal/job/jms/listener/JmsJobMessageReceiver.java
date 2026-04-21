package com.portal.job.jms.listener;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.portal.job.app.service.CompanySyncService;
import com.portal.job.app.service.JobApplicationService;

@Component
@Profile("jms")
public class JmsJobMessageReceiver {

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private CompanySyncService companySyncService;

    @JmsListener(destination = "${queue.job.applied}")
    public void receiveJobApplied(
            @Header("jobId") String jobId,
            @Header("candidateId") String candidateId,
            @Header("candidateName") String candidateName,
            @Header("resumeId") String resumeId) {
        jobApplicationService.createApplicationFromEvent(
                UUID.fromString(jobId), UUID.fromString(candidateId), candidateName, UUID.fromString(resumeId));
    }

    @JmsListener(destination = "${queue.company.updated}")
    public void receiveCompanyUpdated(
            @Header("companyId") String companyId,
            @Header("companyName") String companyName) {
        companySyncService.syncCompanyName(UUID.fromString(companyId), companyName);
    }
}