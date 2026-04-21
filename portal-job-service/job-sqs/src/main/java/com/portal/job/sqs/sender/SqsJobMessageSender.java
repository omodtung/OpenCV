package com.portal.job.sqs.sender;

import com.portal.job.app.messaging.JobMessageSender;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component("jobMessageSender")
@Profile("sqs")
public class SqsJobMessageSender implements JobMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsJobMessageSender.class);

    @Autowired
    private SqsTemplate sqsTemplate;

    @Value("${queue.application.status.out}")
    private String applicationStatusQueue;

    @Override
    public void sendApplicationStatusChanged(String body, Map<String, String> headers) {
        LOGGER.info("Sending application status changed message via SQS");
        sqsTemplate.send(applicationStatusQueue, MessageBuilder.withPayload(body).copyHeaders(headers).build());
    }

    @Override
    public Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, groupId);
        headers.put(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString());
        return headers;
    }
}