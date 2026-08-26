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

            case "ADMIN_ACCESS_REQUEST" -> """
                    %s %s (%s) just registered on AegisZero and requested admin access.

                    Approve: %s

                    Decline: %s

                    This link is single-use and expires in 7 days.
                    """.formatted(data.getOrDefault("firstName", ""), data.getOrDefault("lastName", ""),
                    data.get("email"), data.get("approveUrl"), data.get("rejectUrl"));

            case "ADMIN_ACCESS_APPROVED" -> """
                    Hi %s,

                    Your request for admin access on AegisZero has been approved. Sign in again to pick up the new permissions.
                    """.formatted(data.getOrDefault("firstName", "there"));

            case "ADMIN_ACCESS_REJECTED" -> """
                    Hi %s,

                    Your request for admin access on AegisZero was not approved. Your account remains active with standard access.
                    """.formatted(data.getOrDefault("firstName", "there"));

            default -> "Notification: " + event.type() + "\n\n" + data;
        };
    }

    /**
     * HTML counterpart to {@link #renderBody}, only for the notification
     * types that warrant it. Returns null for everything else, which tells
     * {@link EmailService} to send text-only exactly as before - existing
     * templates (verification, reset, OTP) are unchanged.
     */
    public String renderHtmlBody(NotificationEvent event) {
        Map<String, Object> data = event.templateData() == null ? Map.of() : event.templateData();

        return switch (event.type()) {
            case "ADMIN_ACCESS_REQUEST" -> wrap(
                    "Admin access requested",
                    "<p><strong>%s %s</strong> (%s) just registered on AegisZero and requested admin access.</p>"
                            .formatted(esc(data.getOrDefault("firstName", "")), esc(data.getOrDefault("lastName", "")), esc(data.get("email")))
                            + button("Approve", String.valueOf(data.get("approveUrl")), "#00caff")
                            + button("Decline", String.valueOf(data.get("rejectUrl")), "#ff3b5c")
                            + "<p class=\"muted\">This link is single-use and expires in 7 days.</p>"
            );

            case "ADMIN_ACCESS_APPROVED" -> wrap(
                    "Admin access approved",
                    "<p>Hi %s,</p><p>Your request for admin access on AegisZero has been approved. Sign in again to pick up the new permissions.</p>"
                            .formatted(esc(data.getOrDefault("firstName", "there")))
            );

            case "ADMIN_ACCESS_REJECTED" -> wrap(
                    "Admin access request declined",
                    "<p>Hi %s,</p><p>Your request for admin access on AegisZero was not approved. Your account remains active with standard access.</p>"
                            .formatted(esc(data.getOrDefault("firstName", "there")))
            );

            default -> null;
        };
    }

    private String button(String label, String href, String color) {
        return """
                <a href="%s" style="display:inline-block;margin:8px 12px 0 0;padding:12px 24px;border-radius:10px;background:%s;color:#0a0a0a;font-weight:600;text-decoration:none;font-family:-apple-system,Segoe UI,Roboto,sans-serif;">%s</a>
                """.formatted(href, color, label);
    }

    private String esc(Object value) {
        return String.valueOf(value)
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String wrap(String title, String bodyHtml) {
        return """
                <!doctype html>
                <html><body style="margin:0;padding:32px 16px;background:#0a0a0a;font-family:-apple-system,Segoe UI,Roboto,sans-serif;">
                <div style="max-width:480px;margin:0 auto;padding:32px;border-radius:20px;background:#141416;border:1px solid rgba(255,255,255,0.08);">
                <div style="width:44px;height:44px;border-radius:12px;background:linear-gradient(135deg,#4300ff,#00caff);margin-bottom:20px;"></div>
                <h1 style="color:#f5f5f7;font-size:19px;margin:0 0 16px;">%s</h1>
                <div style="color:#d0d0d6;font-size:14px;line-height:1.6;">%s</div>
                <p style="margin-top:32px;font-size:11px;letter-spacing:0.04em;text-transform:uppercase;color:#6b6b76;">AegisZero</p>
                </div>
                </body></html>
                """.formatted(title, bodyHtml);
    }
}
