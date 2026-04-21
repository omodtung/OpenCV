package com.portal.employer.jms.sender;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.portal.employer.app.messaging.EmployerMessageSender;

import jakarta.jms.TextMessage;

@Component("employerMessageSender")
@Profile("jms")
public class JmsEmployerMessageSender implements EmployerMessageSender {

    private final JmsTemplate jmsTemplate;

    @Value("${queue.company.created}")
    private String companyCreatedQueue;

    @Value("${queue.company.updated}")
    private String companyUpdatedQueue;

    public JmsEmployerMessageSender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void sendCompanyCreated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(companyCreatedQueue, messageBody, messageHeaders);
    }

    @Override
    public void sendCompanyUpdated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(companyUpdatedQueue, messageBody, messageHeaders);
    }

    private void sendMessage(String queue, String messageBody, Map<String, String> messageHeaders) {
        jmsTemplate.send(queue, session -> {
            TextMessage message = session.createTextMessage(messageBody);
            for (Map.Entry<String, String> entry : messageHeaders.entrySet()) {
                message.setStringProperty(entry.getKey(), entry.getValue());
            }
            return message;
        });
    }
}