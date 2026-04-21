package com.portal.identity.jms.sender;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.portal.identity.app.messaging.IdentityMessageSender;

import jakarta.jms.TextMessage;

@Component("identityMessageSender")
@Profile("jms")
public class JmsIdentityMessageSender implements IdentityMessageSender {

    private final JmsTemplate jmsTemplate;

    @Value("${queue.user.registered}")
    private String userRegisteredQueue;

    @Value("${queue.user.deactivated}")
    private String userDeactivatedQueue;

    public JmsIdentityMessageSender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void sendUserRegistered(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(userRegisteredQueue, messageBody, messageHeaders);
    }

    @Override
    public void sendUserDeactivated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(userDeactivatedQueue, messageBody, messageHeaders);
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