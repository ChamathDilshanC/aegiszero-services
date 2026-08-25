package com.aegiszero.notification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Default provider: sends via SMTP (JavaMailSender). Used for local
 * development (Mailhog) and for any deployment target that allows outbound
 * SMTP. Render's free tier does not — see {@link BrevoApiEmailSender}.
 */
@Service
@ConditionalOnProperty(prefix = "email", name = "provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(message.fromEmail());
        mail.setTo(message.toEmail());
        mail.setSubject(message.subject());
        mail.setText(message.textContent() != null ? message.textContent() : message.htmlContent());
        mailSender.send(mail);
    }
}
