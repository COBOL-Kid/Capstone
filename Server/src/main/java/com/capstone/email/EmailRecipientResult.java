package com.capstone.email;

public record EmailRecipientResult(
    String email, String messageUuid, long messageId, String messageHref) {}
