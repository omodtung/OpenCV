package com.portal.candidate.sqs.sender;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.portal.candidate.app.messaging.CandidateMessageSender;

import io.awspring.cloud.sqs.operations.SqsTemplate;

@Component("candidateMessageSender")
@Profile("sqs")
public class SqsCandidateMessageSender implements CandidateMessageSender {

    private final SqsTemplate sqsTemplate;

    @Value("${queue.job.applied}")
    private String jobAppliedQueue;

    @Value("${queue.application.withdrawn:portal-application-withdrawn}")
    private String applicationWithdrawnQueue;

    public SqsCandidateMessageSender(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void sendJobApplied(String messageBody, Map<String, String> messageHeaders) {
        sqsTemplate.send(jobAppliedQueue,
                MessageBuilder.withPayload(messageBody).copyHeaders(messageHeaders).build());
    }

    @Override
    public void sendApplicationWithdrawn(String messageBody, Map<String, String> messageHeaders) {
        sqsTemplate.send(applicationWithdrawnQueue,
                MessageBuilder.withPayload(messageBody).copyHeaders(messageHeaders).build());
    }
}