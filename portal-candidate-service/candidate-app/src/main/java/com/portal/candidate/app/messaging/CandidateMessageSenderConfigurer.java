package com.portal.candidate.app.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CandidateMessageSenderConfigurer {

    @Bean
    @ConditionalOnMissingBean(CandidateMessageSender.class)
    public CandidateMessageSender candidateMessageSender() {
        return new DefaultCandidateMessageSender();
    }
}