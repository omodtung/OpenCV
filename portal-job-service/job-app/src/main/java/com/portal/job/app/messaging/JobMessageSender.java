package com.portal.job.app.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface JobMessageSender {

    String GROUP_ID_HEADER = "message-group-id";
    String DEDUPLICATION_ID_HEADER = "message-deduplication-id";

    void sendApplicationStatusChanged(String body, Map<String, String> headers);

    default Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put(GROUP_ID_HEADER, groupId);
        headers.put(DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString());
        return headers;
    }
}