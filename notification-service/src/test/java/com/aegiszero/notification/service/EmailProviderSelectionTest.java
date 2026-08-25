package com.aegiszero.notification.service;

import com.aegiszero.notification.config.EmailProviderValidator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies email.provider selects exactly one EmailSender implementation,
 * and that each provider's own required config is only enforced when that
 * provider is actually selected — without booting Kafka/mail/DB
 * autoconfiguration, just the beans this decision actually involves.
 */
class EmailProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JavaMailSender.class, () -> Mockito.mock(JavaMailSender.class))
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(SmtpEmailSender.class, BrevoApiEmailSender.class, EmailProviderValidator.class);

    @Test
    void defaultsToSmtpWhenProviderUnset() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SmtpEmailSender.class);
            assertThat(context).doesNotHaveBean(BrevoApiEmailSender.class);
        });
    }

    @Test
    void selectsSmtpExplicitlyAndDoesNotRequireBrevoApiKey() {
        contextRunner.withPropertyValues("email.provider=smtp").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SmtpEmailSender.class);
            assertThat(context).doesNotHaveBean(BrevoApiEmailSender.class);
        });
    }

    @Test
    void selectsBrevoApiAndDoesNotRequireSmtpCredentials() {
        // No SMTP_USERNAME/PASSWORD anywhere in this context at all — proves
        // brevo-api mode has no dependency on them.
        contextRunner
                .withPropertyValues("email.provider=brevo-api", "aegiszero.brevo.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(BrevoApiEmailSender.class);
                    assertThat(context).doesNotHaveBean(SmtpEmailSender.class);
                });
    }

    @Test
    void brevoApiWithoutApiKeyFailsFast() {
        contextRunner
                .withPropertyValues("email.provider=brevo-api")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("BREVO_API_KEY is required when email.provider=brevo-api");
                });
    }

    @Test
    void unknownProviderFailsFastWithClearMessage() {
        contextRunner
                .withPropertyValues("email.provider=carrier-pigeon")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "Unsupported email.provider 'carrier-pigeon' — expected one of [smtp, brevo-api]");
                });
    }
}
