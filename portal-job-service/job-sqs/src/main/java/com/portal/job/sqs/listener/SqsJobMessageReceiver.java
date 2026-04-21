package com.portal.job.sqs.listener;

import com.portal.job.app.service.CompanySyncService;
import com.portal.job.app.service.JobApplicationService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@Profile("sqs")
public class SqsJobMessageReceiver {

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private CompanySyncService companySyncService;

    @SqsListener(value = "${queue.job.applied}")
    public void receiveJobApplied(
            @Header("jobId") String jobId,
            @Header("candidateId") String candidateId,
            @Header("candidateName") String candidateName,
            @Header("resumeId") String resumeId) {
        jobApplicationService.createApplicationFromEvent(
                UUID.fromString(jobId), UUID.fromString(candidateId), candidateName, UUID.fromString(resumeId));
    }

    @SqsListener(value = "${queue.company.updated}")
    public void receiveCompanyUpdated(
            @Header("companyId") String companyId,
            @Header("companyName") String companyName) {
        companySyncService.syncCompanyName(UUID.fromString(companyId), companyName);
    }
}