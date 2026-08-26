package com.aegiszero.notification.service;

import com.aegiszero.common.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailSender emailSender;
    private final EmailTemplateService templateService;
    private final String fromEmail;
    private final String fromName;

    public EmailService(EmailSender emailSender, EmailTemplateService templateService,
                         @Value("${aegiszero.mail-from:no-reply@aegiszero.local}") String fromEmail,
                         @Value("${aegiszero.mail-from-name:AegisZero Security}") String fromName) {
        this.emailSender = emailSender;
        this.templateService = templateService;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    /**
     * Renders and sends the notification. Never throws — a failed send is
     * logged and swallowed so a flaky mail provider can't stall or crash the
     * event listener that calls this.
     */
    public void send(NotificationEvent event) {
        try {
            EmailMessage message = new EmailMessage(
                    fromEmail,
                    fromName,
                    event.recipientEmail(),
                    null,
                    event.subject(),
                    templateService.renderBody(event),
                    templateService.renderHtmlBody(event)
            );
            emailSender.send(message);
            log.info("Sent '{}' notification email to {}", event.type(), event.recipientEmail());
        } catch (Exception e) {
            log.error("Failed to send '{}' notification email to {}: {}", event.type(), event.recipientEmail(), e.getMessage());
        }
    }
}
