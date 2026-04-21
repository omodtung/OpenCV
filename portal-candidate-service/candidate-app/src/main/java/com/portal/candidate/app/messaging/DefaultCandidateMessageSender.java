package com.portal.candidate.app.messaging;

import java.util.Map;

public class DefaultCandidateMessageSender implements CandidateMessageSender {

    @Override
    public void sendJobApplied(String messageBody, Map<String, String> messageHeaders) {}

    @Override
    public void sendApplicationWithdrawn(String messageBody, Map<String, String> messageHeaders) {}
}