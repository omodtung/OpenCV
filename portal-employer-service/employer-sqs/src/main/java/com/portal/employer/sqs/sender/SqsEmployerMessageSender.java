package com.portal.employer.sqs.sender;

import com.portal.employer.app.messaging.EmployerMessageSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("employerMessageSender")
@Profile("sqs")
public class SqsEmployerMessageSender implements EmployerMessageSender {

    private final SqsTemplate sqsTemplate;

    @Value("${queue.company.created}")
    private String companyCreatedQueueArn;

    @Value("${queue.company.updated}")
    private String companyUpdatedQueueArn;

    public SqsEmployerMessageSender(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void sendCompanyCreated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(companyCreatedQueueArn, messageBody, messageHeaders);
    }

    @Override
    public void sendCompanyUpdated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(companyUpdatedQueueArn, messageBody, messageHeaders);
    }

    private void sendMessage(String queueArn, String messageBody, Map<String, String> messageHeaders) {
        String queueName = queueArn.contains(":") ? queueArn.substring(queueArn.lastIndexOf(":") + 1) : queueArn;
        sqsTemplate.send(to -> to.queue(queueName).payload(messageBody).headers(new java.util.HashMap<>(messageHeaders)));
    }
}