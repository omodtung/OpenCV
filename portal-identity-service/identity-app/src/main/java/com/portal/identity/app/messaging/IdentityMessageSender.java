package com.portal.identity.app.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface IdentityMessageSender {

    void sendUserRegistered(String messageBody, Map<String, String> messageHeaders);

    void sendUserDeactivated(String messageBody, Map<String, String> messageHeaders);

    default Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("message-group-id", groupId);
        headers.put("message-deduplication-id", UUID.randomUUID().toString());
        return headers;
    }
}