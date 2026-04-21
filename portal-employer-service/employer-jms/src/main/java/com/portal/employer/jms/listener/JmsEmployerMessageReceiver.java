package com.portal.employer.jms.listener;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.portal.employer.app.service.EmployerProfileService;

@Component
@Profile("jms")
public class JmsEmployerMessageReceiver {

    @Autowired
    private EmployerProfileService employerProfileService;

    @JmsListener(destination = "${queue.user.registered}")
    public void receiveUserRegistered(
            @Header("userId") String userId,
            @Header("email") String email,
            @Header("role") String role) {
        if ("ROLE_EMPLOYER".equals(role)) {
            employerProfileService.createProfile(UUID.fromString(userId), "New Company");
        }
    }
}
