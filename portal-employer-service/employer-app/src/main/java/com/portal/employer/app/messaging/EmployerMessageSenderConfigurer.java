package com.portal.employer.app.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployerMessageSenderConfigurer {

    @Bean
    @ConditionalOnMissingBean(EmployerMessageSender.class)
    public EmployerMessageSender employerMessageSender() {
        return new DefaultEmployerMessageSender();
    }
}