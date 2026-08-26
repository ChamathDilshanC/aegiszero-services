package com.aegiszero.notification.service;

import com.aegiszero.common.event.NotificationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceTest {

    private final EmailTemplateService service = new EmailTemplateService("https://app.example.com");

    @Test
    void adminAccessRequestHtmlCarriesBothLinksAndEscapesName() {
        NotificationEvent event = new NotificationEvent("owner@example.com", "ADMIN_ACCESS_REQUEST", "subject",
                Map.of(
                        "firstName", "<script>alert(1)</script>",
                        "lastName", "Doe",
                        "email", "new-admin@example.com",
                        "approveUrl", "https://auth.example.com/api/auth/admin-requests/1/approve?token=abc",
                        "rejectUrl", "https://auth.example.com/api/auth/admin-requests/1/reject?token=def"
                ), Instant.now());

        String html = service.renderHtmlBody(event);

        assertThat(html).contains("https://auth.example.com/api/auth/admin-requests/1/approve?token=abc");
        assertThat(html).contains("https://auth.example.com/api/auth/admin-requests/1/reject?token=def");
        assertThat(html).contains("new-admin@example.com");
        assertThat(html).doesNotContain("<script>alert(1)</script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void adminAccessRequestTextFallbackCarriesBothLinks() {
        NotificationEvent event = new NotificationEvent("owner@example.com", "ADMIN_ACCESS_REQUEST", "subject",
                Map.of(
                        "firstName", "Jane", "lastName", "Doe", "email", "new-admin@example.com",
                        "approveUrl", "https://auth.example.com/approve", "rejectUrl", "https://auth.example.com/reject"
                ), Instant.now());

        String text = service.renderBody(event);

        assertThat(text).contains("https://auth.example.com/approve");
        assertThat(text).contains("https://auth.example.com/reject");
        assertThat(text).contains("Jane Doe");
    }

    @Test
    void adminAccessApprovedAndRejectedRenderBothFormats() {
        NotificationEvent approved = new NotificationEvent("user@example.com", "ADMIN_ACCESS_APPROVED", "subject",
                Map.of("firstName", "Jane"), Instant.now());
        NotificationEvent rejected = new NotificationEvent("user@example.com", "ADMIN_ACCESS_REJECTED", "subject",
                Map.of("firstName", "Jane"), Instant.now());

        assertThat(service.renderBody(approved)).contains("approved");
        assertThat(service.renderHtmlBody(approved)).contains("approved");
        assertThat(service.renderBody(rejected)).contains("not approved");
        assertThat(service.renderHtmlBody(rejected)).contains("declined");
    }

    @Test
    void existingTemplatesStayTextOnly() {
        NotificationEvent verification = new NotificationEvent("user@example.com", "EMAIL_VERIFICATION", "subject",
                Map.of("token", "tok", "firstName", "Jane"), Instant.now());

        assertThat(service.renderBody(verification)).contains("https://app.example.com/verify-email?token=tok");
        assertThat(service.renderHtmlBody(verification)).isNull();
    }
}
