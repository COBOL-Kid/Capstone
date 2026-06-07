package com.capstone.email;

import java.util.List;

public record EmailSendResult(String status, List<EmailRecipientResult> to) {}
