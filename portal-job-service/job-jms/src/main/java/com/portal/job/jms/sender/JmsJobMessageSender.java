package com.portal.job.jms.sender;

import com.portal.job.app.messaging.JobMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component("jobMessageSender")
@Profile("jms")
public class JmsJobMessageSender implements JobMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmsJobMessageSender.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${queue.application.status.out}")
    private String applicationStatusQueue;

    @Override
    public void sendApplicationStatusChanged(String body, Map<String, String> headers) {
        LOGGER.info("Sending application status changed message");
        jmsTemplate.send(applicationStatusQueue, session -> {
            var message = session.createTextMessage(body);
            for (var entry : headers.entrySet()) {
                message.setStringProperty(entry.getKey(), entry.getValue());
            }
            return message;
        });
    }
}