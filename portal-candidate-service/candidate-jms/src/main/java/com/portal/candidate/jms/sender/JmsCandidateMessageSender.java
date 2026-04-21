package com.portal.candidate.jms.sender;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.portal.candidate.app.messaging.CandidateMessageSender;

import jakarta.jms.JMSException;

@Component("candidateMessageSender")
@Profile("jms")
public class JmsCandidateMessageSender implements CandidateMessageSender {

    private final JmsTemplate jmsTemplate;

    @Value("${queue.job.applied}")
    private String jobAppliedQueue;

    @Value("${queue.application.withdrawn:portal-application-withdrawn}")
    private String applicationWithdrawnQueue;

    public JmsCandidateMessageSender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void sendJobApplied(String messageBody, Map<String, String> messageHeaders) {
        jmsTemplate.send(jobAppliedQueue, session -> {
            var msg = session.createTextMessage(messageBody);
            messageHeaders.forEach((k, v) -> {
                try { msg.setStringProperty(k, v); } catch (JMSException e) { throw new RuntimeException(e); }
            });
            return msg;
        });
    }

    @Override
    public void sendApplicationWithdrawn(String messageBody, Map<String, String> messageHeaders) {
        jmsTemplate.send(applicationWithdrawnQueue, session -> {
            var msg = session.createTextMessage(messageBody);
            messageHeaders.forEach((k, v) -> {
                try { msg.setStringProperty(k, v); } catch (JMSException e) { throw new RuntimeException(e); }
            });
            return msg;
        });
    }
}