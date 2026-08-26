package com.aegiszero.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        if (StringUtils.hasText(message.htmlContent())) {
            sendHtml(message);
            return;
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(message.fromEmail());
        mail.setTo(message.toEmail());
        mail.setSubject(message.subject());
        mail.setText(message.textContent());
        mailSender.send(mail);
    }

    private void sendHtml(EmailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true);
            helper.setFrom(message.fromEmail());
            helper.setTo(message.toEmail());
            helper.setSubject(message.subject());
            String text = StringUtils.hasText(message.textContent()) ? message.textContent() : message.htmlContent();
            helper.setText(text, message.htmlContent());
            mailSender.send(mime);
        } catch (jakarta.mail.MessagingException ex) {
            throw new IllegalStateException("Failed to build HTML email", ex);
        }
    }
}
