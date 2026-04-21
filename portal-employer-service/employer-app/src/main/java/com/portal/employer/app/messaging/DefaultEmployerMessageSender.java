package com.portal.employer.app.messaging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEmployerMessageSender implements EmployerMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultEmployerMessageSender.class);

    @Override
    public void sendCompanyCreated(String messageBody, Map<String, String> messageHeaders) {
        LOG.warn("No messaging provider configured - sendCompanyCreated not sent");
    }

    @Override
    public void sendCompanyUpdated(String messageBody, Map<String, String> messageHeaders) {
        LOG.warn("No messaging provider configured - sendCompanyUpdated not sent");
    }
}
