package com.capstone.integration;

public record EmailRecipientResult(
    String email, String messageUuid, long messageId, String messageHref) {}
