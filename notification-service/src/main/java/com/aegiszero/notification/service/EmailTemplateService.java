package com.aegiszero.notification.service;

import com.aegiszero.common.event.NotificationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmailTemplateService {

    private final String frontendBaseUrl;

    public EmailTemplateService(@Value("${aegiszero.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String renderBody(NotificationEvent event) {
        Map<String, Object> data = event.templateData() == null ? Map.of() : event.templateData();

        return switch (event.type()) {
            case "EMAIL_VERIFICATION" -> """
                    Hi %s,

                    Welcome to AegisZero. Please verify your email address by opening the link below:

                    %s/verify-email?token=%s

                    This link expires in 24 hours. If you didn't create this account, you can ignore this email.
                    """.formatted(data.getOrDefault("firstName", "there"), frontendBaseUrl, data.get("token"));

            case "PASSWORD_RESET" -> """
                    Hi %s,

                    We received a request to reset your AegisZero password. Open the link below to choose a new one:

                    %s/reset-password?token=%s

                    This link expires in 30 minutes. If you didn't request this, you can safely ignore this email.
                    """.formatted(data.getOrDefault("firstName", "there"), frontendBaseUrl, data.get("token"));

            case "MFA_OTP" -> """
                    Your AegisZero verification code is: %s

                    This code expires in 5 minutes. If you didn't try to sign in, please secure your account.
                    """.formatted(data.get("code"));

            default -> "Notification: " + event.type() + "\n\n" + data;
        };
    }
}
