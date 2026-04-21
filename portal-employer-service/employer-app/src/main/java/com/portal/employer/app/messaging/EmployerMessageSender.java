package com.portal.employer.app.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID
public interface EmployerMessageSender {

    void sendCompanyCreated(String messageBody, Map<String, String> messageHeaders);

    void sendCompanyUpdated(String messageBody, Map<String, String> messageHeaders);

    default Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("message-group-id", groupId);
        headers.put("message-deduplication-id", UUID.randomUUID().toString());
        return headers;
    }
}