package com.portal.identity.app.messaging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultIdentityMessageSender implements IdentityMessageSender {

    private static final Logger log = LoggerFactory.getLogger(DefaultIdentityMessageSender.class);

    @Override
    public void sendUserRegistered(String messageBody, Map<String, String> messageHeaders) {
        log.info("No-op sendUserRegistered: {}", messageBody);
    }

    @Override
    public void sendUserDeactivated(String messageBody, Map<String, String> messageHeaders) {
        log.info("No-op sendUserDeactivated: {}", messageBody);
    }
}
