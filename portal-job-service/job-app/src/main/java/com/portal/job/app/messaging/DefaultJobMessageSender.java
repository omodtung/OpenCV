package com.portal.job.app.messaging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJobMessageSender implements JobMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJobMessageSender.class);

    @Override
    public void sendApplicationStatusChanged(String body, Map<String, String> headers) {
        LOGGER.warn("Message is not sent... Body:{}, Headers:{}", body, headers);
    }
}
