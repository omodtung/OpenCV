package com.portal.employer.sqs.listener;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.portal.employer.app.service.EmployerProfileService;

import io.awspring.cloud.sqs.annotation.SqsListener;

@Component
@Profile("sqs")
public class SqsEmployerMessageReceiver {

    @Autowired
    private EmployerProfileService employerProfileService;

    @SqsListener(value = "#{'${queue.user.registered}'.contains('arn:') ? '${queue.user.registered}'.substring('${queue.user.registered}'.lastIndexOf(':') + 1) : '${queue.user.registered}'}")
    public void receiveUserRegistered(
            @Header("userId") String userId,
            @Header("email") String email,
            @Header("role") String role) {
        if ("ROLE_EMPLOYER".equals(role)) {
            employerProfileService.createProfile(UUID.fromString(userId), "New Company");
        }
    }
}
