package com.portal.job.app.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobMessageSenderConfigurer {

    @Bean
    @ConditionalOnMissingBean(JobMessageSender.class)
    public JobMessageSender jobMessageSender() {
        return new DefaultJobMessageSender();
    }
}