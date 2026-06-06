package com.capstone.integration;

import java.util.List;

public record EmailSendResult(String status, List<EmailRecipientResult> to) {}
