package com.aegiszero.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrevoApiEmailSenderTest {

    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String API_KEY = "xkeysib-test-key-not-real";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private BrevoApiEmailSender sender() {
        return new BrevoApiEmailSender(builder, API_URL, API_KEY);
    }

    @Test
    void constructorRejectsBlankApiKey() {
        assertThatThrownBy(() -> new BrevoApiEmailSender(RestClient.builder(), API_URL, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BREVO_API_KEY");
    }

    @Test
    void sendsCorrectRequestShapeAndApiKeyHeader() {
        server.expect(requestTo(API_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.sender.email").value("no-reply@aegiszero.local"))
                .andExpect(jsonPath("$.sender.name").value("AegisZero Security"))
                .andExpect(jsonPath("$.to[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.to[0].name").doesNotExist())
                .andExpect(jsonPath("$.subject").value("Verify your email"))
                .andExpect(jsonPath("$.textContent").value("Please verify your account."))
                .andExpect(jsonPath("$.htmlContent").doesNotExist())
                .andRespond(withSuccess("{\"messageId\":\"abc123\"}", MediaType.APPLICATION_JSON));

        EmailMessage message = new EmailMessage(
                "no-reply@aegiszero.local", "AegisZero Security",
                "user@example.com", null,
                "Verify your email",
                "Please verify your account.", null);

        sender().send(message);

        server.verify();
    }

    @Test
    void includesRecipientNameWhenPresent() {
        server.expect(requestTo(API_URL))
                .andExpect(jsonPath("$.to[0].name").value("Jane Doe"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        EmailMessage message = new EmailMessage(
                "no-reply@aegiszero.local", "AegisZero Security",
                "jane@example.com", "Jane Doe",
                "Hello", "Hi Jane", null);

        sender().send(message);

        server.verify();
    }

    @Test
    void wrapsNon2xxResponseInIllegalStateException() {
        server.expect(requestTo(API_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Key not found\"}"));

        EmailMessage message = new EmailMessage(
                "no-reply@aegiszero.local", "AegisZero Security",
                "user@example.com", null,
                "Subject", "Body", null);

        assertThatThrownBy(() -> sender().send(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Brevo API send failed")
                .hasMessageContaining("401");

        server.verify();
    }
}
