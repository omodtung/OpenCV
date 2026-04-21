package com.portal.identity.app.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityMessageSenderConfigurer {

    @Bean
    @ConditionalOnMissingBean(IdentityMessageSender.class)
    public IdentityMessageSender identityMessageSender() {
        return new DefaultIdentityMessageSender();
    }
}