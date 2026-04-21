package com.portal.identity.sqs.sender;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.portal.identity.app.messaging.IdentityMessageSender;

import io.awspring.cloud.sqs.operations.SqsTemplate;

@Component("identityMessageSender")
@Profile("sqs")
public class SqsIdentityMessageSender implements IdentityMessageSender {

    private final SqsTemplate sqsTemplate;

    @Value("${queue.user.registered}")
    private String userRegisteredQueueArn;

    @Value("${queue.user.deactivated}")
    private String userDeactivatedQueueArn;

    public SqsIdentityMessageSender(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void sendUserRegistered(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(userRegisteredQueueArn, messageBody, messageHeaders);
    }

    @Override
    public void sendUserDeactivated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(userDeactivatedQueueArn, messageBody, messageHeaders);
    }

    private void sendMessage(String queueArn, String messageBody, Map<String, String> messageHeaders) {
        String queueName = extractQueueName(queueArn);
        sqsTemplate.send(to -> to
                .queue(queueName)
                .payload(messageBody)
                .headers(new java.util.HashMap<>(messageHeaders))
        );
    }

    private String extractQueueName(String arn) {
        if (arn != null && arn.contains(":")) {
            return arn.substring(arn.lastIndexOf(":") + 1);
        }
        return arn;
    }
}