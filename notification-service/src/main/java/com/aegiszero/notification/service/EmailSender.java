package com.aegiszero.notification.service;

/**
 * Provider abstraction for actually delivering an email. Exactly one
 * implementation is active at a time, selected by {@code email.provider}
 * (see {@link SmtpEmailSender} and {@link BrevoApiEmailSender}).
 */
public interface EmailSender {

    void send(EmailMessage message);
}
