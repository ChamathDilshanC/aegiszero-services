package com.aegiszero.notification.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Sends via the Brevo transactional email REST API (HTTPS, port 443)
 * instead of SMTP. Render's free tier blocks outbound SMTP ports
 * (25/465/587) entirely, so this is the provider that actually works there
 * — see {@link SmtpEmailSender} for anywhere SMTP is reachable.
 */
@Service
@ConditionalOnProperty(prefix = "email", name = "provider", havingValue = "brevo-api")
public class BrevoApiEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoApiEmailSender.class);

    private final RestClient restClient;
    private final String apiKey;

    public BrevoApiEmailSender(RestClient.Builder restClientBuilder,
                                @Value("${aegiszero.brevo.api-url:https://api.brevo.com/v3/smtp/email}") String apiUrl,
                                @Value("${aegiszero.brevo.api-key:}") String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "BREVO_API_KEY is required when email.provider=brevo-api");
        }
        this.apiKey = apiKey;
        this.restClient = restClientBuilder.baseUrl(apiUrl).build();
    }

    @Override
    public void send(EmailMessage message) {
        BrevoEmailRequest request = new BrevoEmailRequest(
                new BrevoParty(message.fromName(), message.fromEmail()),
                List.of(new BrevoParty(message.toName(), message.toEmail())),
                message.subject(),
                message.htmlContent(),
                message.textContent()
        );

        try {
            restClient.post()
                    // Never log this header — it's the only secret in this class.
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent email via Brevo API to {}", message.toEmail());
        } catch (RestClientResponseException ex) {
            log.error("Brevo API rejected email to {}: HTTP {} {}",
                    message.toEmail(), ex.getStatusCode().value(), ex.getStatusText());
            throw new IllegalStateException("Brevo API send failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (Exception ex) {
            log.error("Brevo API call failed for email to {}: {}", message.toEmail(), ex.getMessage());
            throw new IllegalStateException("Brevo API send failed", ex);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BrevoParty(String name, String email) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BrevoEmailRequest(
            BrevoParty sender,
            List<BrevoParty> to,
            String subject,
            String htmlContent,
            String textContent
    ) {
    }
}
