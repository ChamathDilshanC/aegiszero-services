package com.aegiszero.notification.service;

/**
 * A ready-to-send email, decoupled from the {@code NotificationEvent}
 * shape so {@link EmailSender} implementations don't need to know anything
 * about the event/template layer above them.
 */
public record EmailMessage(
        String fromEmail,
        String fromName,
        String toEmail,
        String toName,
        String subject,
        String textContent,
        String htmlContent
) {
}
